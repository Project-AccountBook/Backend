package com.chaewookim.accountbookformoms.domain.asset.application;

import com.chaewookim.accountbookformoms.domain.asset.dao.AccountRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionCategoryRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.dto.request.TransactionRequest;
import com.chaewookim.accountbookformoms.domain.asset.dto.response.TransactionResponse;
import com.chaewookim.accountbookformoms.domain.asset.entity.Account;
import com.chaewookim.accountbookformoms.domain.asset.entity.Transaction;
import com.chaewookim.accountbookformoms.domain.asset.entity.TransactionCategory;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
import com.chaewookim.accountbookformoms.domain.asset.error.AssetErrorCode;
import com.chaewookim.accountbookformoms.domain.budget.event.BudgetExceededCheckEvent;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionCategoryRepository categoryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CacheManager cacheManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CacheEvict(value = "dashboard", key = "#userId + ':' + #request.transactionDate.format(T(java.time.format.DateTimeFormatter).ofPattern('yyyy-MM'))")
    public Long createTransaction(Long userId, TransactionRequest request) {
        if (request.type() == TransactionType.TRANSFER) {
            return createTransferTransaction(userId, request);
        }
        return createNormalTransaction(userId, request);
    }

    // 이체 전용 로직
    private Long createTransferTransaction(Long userId, TransactionRequest request) {

        if (request.accountId().equals(request.targetAccountId())) {
            throw new CustomException(AssetErrorCode.TRANSFER_TO_SELF_FORBIDDEN);
        }

        Account first = accountRepository.findByIdWithLock(Math.min(request.accountId(), request.targetAccountId()))
                .orElseThrow(() -> new CustomException(AssetErrorCode.ACCOUNT_NOT_FOUND));
        Account second = accountRepository.findByIdWithLock(Math.max(request.accountId(), request.targetAccountId()))
                .orElseThrow(() -> new CustomException(AssetErrorCode.ACCOUNT_NOT_FOUND));

        Account source = (request.accountId().equals(first.getId())) ? first : second;
        Account target = (request.targetAccountId().equals(first.getId())) ? first : second;

        if (!source.getUser().getId().equals(userId)) {
            throw new CustomException(AssetErrorCode.TRANSACTION_FORBIDDEN);
        }

        source.changeBalance(request.amount().negate());
        target.changeBalance(request.amount());

        return saveTransferTransaction(source, target, request);
    }

    // 일반 거래 전용 로직
    private Long createNormalTransaction(Long userId, TransactionRequest request) {

        Account account = accountRepository.findByIdWithLock(request.accountId())
                .orElseThrow(() -> new CustomException(AssetErrorCode.ACCOUNT_NOT_FOUND));

        if (!account.getUser().getId().equals(userId)) {
            throw new CustomException(AssetErrorCode.TRANSACTION_FORBIDDEN);
        }

        BigDecimal amount = (request.type() == TransactionType.EXPENSE) ? request.amount().negate() : request.amount();
        account.changeBalance(amount);

        return saveTransaction(account, account, request);
    }

    // 거래 내역 저장 로직
    private Long saveTransaction(Account userAccount, Account transactionAccount, TransactionRequest request) {

        TransactionCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new CustomException(AssetErrorCode.CATEGORY_NOT_FOUND));

        Transaction transaction = Transaction.builder()
                .user(userAccount.getUser())
                .account(transactionAccount)
                .targetAccount(null)
                .transactionCategory(category)
                .type(request.type())
                .amount(request.amount())
                .transactionDate(request.transactionDate())
                .description(request.description())
                .build();

        if (request.type() == TransactionType.EXPENSE) {
            String yearMonth = request.transactionDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            eventPublisher.publishEvent(new BudgetExceededCheckEvent(userAccount.getUser().getId(), yearMonth, request.categoryId()));
        }

        return transactionRepository.save(transaction).getId();
    }

    private Long saveTransferTransaction(Account source, Account target, TransactionRequest request) {

        TransactionCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new CustomException(AssetErrorCode.CATEGORY_NOT_FOUND));

        Transaction transaction = Transaction.builder()
                .user(source.getUser())
                .account(source)
                .targetAccount(target)
                .transactionCategory(category)
                .type(request.type())
                .amount(request.amount())
                .transactionDate(request.transactionDate())
                .description(request.description())
                .build();

        return transactionRepository.save(transaction).getId();
    }

    public Page<TransactionResponse> getTransactions(Long userId, Long accountId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return transactionRepository.findAllByUserIdAndAccountIdAndTransactionDateBetween(userId, accountId, startDate, endDate, pageable)
                .map(TransactionResponse::from);
    }

    public TransactionResponse getTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new CustomException(AssetErrorCode.TRANSACTION_NOT_FOUND));
        return TransactionResponse.from(transaction);
    }

    @Transactional
    public void updateTransaction(Long userId, Long transactionId, TransactionRequest request) {

        Transaction transaction = validateAndGet(userId, transactionId);
        LocalDate previousDate = transaction.getTransactionDate();

        reverseTransactionBalances(transaction);

        TransactionCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new CustomException(AssetErrorCode.CATEGORY_NOT_FOUND));

        if (request.type() == TransactionType.TRANSFER) {
            if (request.targetAccountId() == null || request.accountId().equals(request.targetAccountId())) {
                throw new CustomException(AssetErrorCode.TRANSFER_TO_SELF_FORBIDDEN);
            }

            Account first = accountRepository.findByIdWithLock(Math.min(request.accountId(), request.targetAccountId()))
                    .orElseThrow(() -> new CustomException(AssetErrorCode.ACCOUNT_NOT_FOUND));
            Account second = accountRepository.findByIdWithLock(Math.max(request.accountId(), request.targetAccountId()))
                    .orElseThrow(() -> new CustomException(AssetErrorCode.ACCOUNT_NOT_FOUND));

            Account source = request.accountId().equals(first.getId()) ? first : second;
            Account target = request.targetAccountId().equals(first.getId()) ? first : second;

            if (!source.getUser().getId().equals(userId)) {
                throw new CustomException(AssetErrorCode.TRANSACTION_FORBIDDEN);
            }

            transaction.update(request, category, source, target);
            source.changeBalance(request.amount().negate());
            target.changeBalance(request.amount());
        } else {
            Account account = accountRepository.findByIdWithLock(request.accountId())
                    .orElseThrow(() -> new CustomException(AssetErrorCode.ACCOUNT_NOT_FOUND));

            if (!account.getUser().getId().equals(userId)) {
                throw new CustomException(AssetErrorCode.TRANSACTION_FORBIDDEN);
            }

            transaction.update(request, category, account, null);
            BigDecimal amount = (request.type() == TransactionType.EXPENSE) ? request.amount().negate() : request.amount();
            account.changeBalance(amount);
        }

        evictDashboardCache(userId, previousDate);
        evictDashboardCache(userId, request.transactionDate());
    }

    @Transactional
    public void deleteTransaction(Long userId, Long transactionId) {

        Transaction transaction = validateAndGet(userId, transactionId);

        reverseTransactionBalances(transaction);
        transactionRepository.delete(transaction);

        evictDashboardCache(userId, transaction.getTransactionDate());
    }

    private void reverseTransactionBalances(Transaction transaction) {
        Account source = accountRepository.findByIdWithLock(transaction.getAccount().getId())
                .orElseThrow(() -> new CustomException(AssetErrorCode.ACCOUNT_NOT_FOUND));

        if (transaction.getType() == TransactionType.TRANSFER && transaction.getTargetAccount() != null) {
            source.changeBalance(transaction.getAmount());
            Account target = accountRepository.findByIdWithLock(transaction.getTargetAccount().getId())
                    .orElseThrow(() -> new CustomException(AssetErrorCode.ACCOUNT_NOT_FOUND));
            target.changeBalance(transaction.getAmount().negate());
            return;
        }

        source.changeBalance(transaction.getBalanceChangeAmount().negate());
    }

    // 공통 검증 로직
    private Transaction validateAndGet(Long userId, Long transactionId) {

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new CustomException(AssetErrorCode.TRANSACTION_NOT_FOUND));

        if (!transaction.getUser().getId().equals(userId)) {
            throw new CustomException(AssetErrorCode.TRANSACTION_FORBIDDEN);
        }

        return transaction;
    }

    // 대시보드 캐시 삭제
    private void evictDashboardCache(Long userId, LocalDate date) {

        if (cacheManager.getCache("dashboard") == null) {
            return;
        }

        Objects.requireNonNull(cacheManager.getCache("dashboard"))
                .evict(userId + ":" + date.format(YEAR_MONTH_FORMATTER));
    }
}
