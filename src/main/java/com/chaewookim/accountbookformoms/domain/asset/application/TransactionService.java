package com.chaewookim.accountbookformoms.domain.asset.application;

import com.chaewookim.accountbookformoms.domain.asset.dao.AccountRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionCategoryRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.dto.request.TransactionRequest;
import com.chaewookim.accountbookformoms.domain.asset.dto.response.TransactionResponse;
import com.chaewookim.accountbookformoms.domain.asset.entity.Account;
import com.chaewookim.accountbookformoms.domain.asset.entity.Transaction;
import com.chaewookim.accountbookformoms.domain.asset.entity.TransactionCategory;
import com.chaewookim.accountbookformoms.domain.asset.enums.AccountKind;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
import com.chaewookim.accountbookformoms.domain.asset.error.AssetErrorCode;
import com.chaewookim.accountbookformoms.domain.budget.event.BudgetExceededCheckEvent;
import com.chaewookim.accountbookformoms.domain.asset.event.GoalAchievedCheckEvent;
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
        return createTransaction(userId, request, false);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CacheEvict(value = "dashboard", key = "#userId + ':' + #request.transactionDate.format(T(java.time.format.DateTimeFormatter).ofPattern('yyyy-MM'))")
    public void createTransactionFromFixed(Long userId, TransactionRequest request) {
        createTransaction(userId, request, true);
    }

    private Long createTransaction(Long userId, TransactionRequest request, boolean fromFixedTransaction) {
        if (request.type() == TransactionType.TRANSFER) {
            return createTransferTransaction(userId, request, fromFixedTransaction);
        }
        return createNormalTransaction(userId, request, fromFixedTransaction);
    }

    // 이체 전용 로직
    private Long createTransferTransaction(Long userId, TransactionRequest request, boolean fromFixedTransaction) {

        if (request.targetAccountId() == null) {
            throw new CustomException(AssetErrorCode.TARGET_ACCOUNT_REQUIRED);
        }

        if (request.accountId().equals(request.targetAccountId())) {
            throw new CustomException(AssetErrorCode.TRANSFER_TO_SELF_FORBIDDEN);
        }

        Account first = accountRepository.findByIdWithLock(Math.min(request.accountId(), request.targetAccountId()))
                .orElseThrow(() -> new CustomException(AssetErrorCode.ACCOUNT_NOT_FOUND));
        Account second = accountRepository.findByIdWithLock(Math.max(request.accountId(), request.targetAccountId()))
                .orElseThrow(() -> new CustomException(AssetErrorCode.ACCOUNT_NOT_FOUND));

        Account source = (request.accountId().equals(first.getId())) ? first : second;
        Account target = (request.targetAccountId().equals(first.getId())) ? first : second;

        if (!source.getUser().getId().equals(userId)
                || !target.getUser().getId().equals(userId)) {
            throw new CustomException(AssetErrorCode.TRANSACTION_FORBIDDEN);
        }

        applyTransferBalances(source, target, request.amount());

        resetGoalAchievementStateIfNeeded(source);
        resetGoalAchievementStateIfNeeded(target);

        publishGoalAchievedCheck(source.getUser().getId(), source.getId());
        publishGoalAchievedCheck(target.getUser().getId(), target.getId());

        return saveTransferTransaction(source, target, request, fromFixedTransaction);
    }

    // 일반 거래 전용 로직
    private Long createNormalTransaction(Long userId, TransactionRequest request, boolean fromFixedTransaction) {

        Account account = accountRepository.findByIdWithLock(request.accountId())
                .orElseThrow(() -> new CustomException(AssetErrorCode.ACCOUNT_NOT_FOUND));

        if (!account.getUser().getId().equals(userId)) {
            throw new CustomException(AssetErrorCode.TRANSACTION_FORBIDDEN);
        }

        validateNormalTransactionType(account, request.type());
        BigDecimal amount = (request.type() == TransactionType.EXPENSE) ? request.amount().negate() : request.amount();
        account.changeBalance(amount);

        resetGoalAchievementStateIfNeeded(account);

        publishGoalAchievedCheck(userId, account.getId());

        return saveTransaction(account, account, request, fromFixedTransaction);
    }

    // 거래 내역 저장 로직
    private Long saveTransaction(Account userAccount, Account transactionAccount, TransactionRequest request,
                                 boolean fromFixedTransaction) {

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

        if (fromFixedTransaction) {
            transaction.markAsFixedTransactionGenerated();
        }

        Long savedId = transactionRepository.save(transaction).getId();

        if (request.type() == TransactionType.EXPENSE) {
            String yearMonth = request.transactionDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            eventPublisher.publishEvent(new BudgetExceededCheckEvent(userAccount.getUser().getId(), yearMonth, request.categoryId()));
        }

        return savedId;
    }

    private Long saveTransferTransaction(Account source, Account target, TransactionRequest request,
                                         boolean fromFixedTransaction) {

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

        if (fromFixedTransaction) {
            transaction.markAsFixedTransactionGenerated();
        }

        return transactionRepository.save(transaction).getId();
    }

    public Page<TransactionResponse> getTransactions(Long userId, Long accountId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return transactionRepository.findAllByUserIdAndAccountIdAndTransactionDateBetween(userId, accountId, startDate, endDate, pageable)
                .map(TransactionResponse::from);
    }

    public Page<TransactionResponse> getAllUserTransactions(Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return transactionRepository.findAllByUserIdAndTransactionDateBetween(userId, startDate, endDate, pageable)
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

            if (!source.getUser().getId().equals(userId)
                    || !target.getUser().getId().equals(userId)) {
                throw new CustomException(AssetErrorCode.TRANSACTION_FORBIDDEN);
            }

            transaction.update(request, category, source, target);
            applyTransferBalances(source, target, request.amount());
            resetGoalAchievementStateIfNeeded(source);
            resetGoalAchievementStateIfNeeded(target);
            publishGoalAchievedCheck(userId, source.getId());
            publishGoalAchievedCheck(target.getUser().getId(), target.getId());
        } else {
            Account account = accountRepository.findByIdWithLock(request.accountId())
                    .orElseThrow(() -> new CustomException(AssetErrorCode.ACCOUNT_NOT_FOUND));

            if (!account.getUser().getId().equals(userId)) {
                throw new CustomException(AssetErrorCode.TRANSACTION_FORBIDDEN);
            }

            validateNormalTransactionType(account, request.type());
            transaction.update(request, category, account, null);
            BigDecimal amount = (request.type() == TransactionType.EXPENSE) ? request.amount().negate() : request.amount();
            account.changeBalance(amount);
            resetGoalAchievementStateIfNeeded(account);
            publishGoalAchievedCheck(userId, account.getId());
        }

        evictDashboardCache(userId, previousDate);
        evictDashboardCache(userId, request.transactionDate());
    }

    @Transactional
    public void deleteTransaction(Long userId, Long transactionId) {

        Transaction transaction = validateAndGet(userId, transactionId);

        reverseTransactionBalances(transaction);
        publishGoalChecksAfterReverse(transaction);
        transactionRepository.delete(transaction);

        evictDashboardCache(userId, transaction.getTransactionDate());
    }

    private void reverseTransactionBalances(Transaction transaction) {
        Account source = accountRepository.findByIdWithLock(transaction.getAccount().getId())
                .orElseThrow(() -> new CustomException(AssetErrorCode.ACCOUNT_NOT_FOUND));

        if (transaction.getType() == TransactionType.TRANSFER && transaction.getTargetAccount() != null) {
            Account target = accountRepository.findByIdWithLock(transaction.getTargetAccount().getId())
                    .orElseThrow(() -> new CustomException(AssetErrorCode.ACCOUNT_NOT_FOUND));
            reverseTransferBalances(source, target, transaction.getAmount());
            resetGoalAchievementStateIfNeeded(source);
            resetGoalAchievementStateIfNeeded(target);
            return;
        }

        source.changeBalance(transaction.getBalanceChangeAmount().negate());
        resetGoalAchievementStateIfNeeded(source);
    }

    private void resetGoalAchievementStateIfNeeded(Account account) {
        if (account.getGoalAmount() == null) {
            return;
        }
        if (!account.isGoalAchieved() && account.isGoalAchievedNotified()) {
            account.resetGoalAchievedNotified();
        }
    }

    private void validateNormalTransactionType(Account account, TransactionType type) {
        if (account.getKind() == AccountKind.LOAN) {
            throw new CustomException(AssetErrorCode.NORMAL_TRANSACTION_NOT_ALLOWED_FOR_LOAN);
        }
        if (type == TransactionType.INCOME && account.getKind() != AccountKind.ASSET) {
            throw new CustomException(AssetErrorCode.INCOME_NOT_ALLOWED_FOR_LIABILITY);
        }
    }

    private void applyTransferBalances(Account source, Account target, BigDecimal amount) {
        if (source.getKind() == AccountKind.LOAN) {
            if (target.getKind() != AccountKind.ASSET) {
                throw new CustomException(AssetErrorCode.INVALID_LOAN_TRANSFER);
            }
            source.disburse(amount);
            target.changeBalance(amount);
            return;
        }
        if (target.getKind() == AccountKind.LOAN) {
            if (source.getKind() != AccountKind.ASSET) {
                throw new CustomException(AssetErrorCode.INVALID_LOAN_TRANSFER);
            }
            source.changeBalance(amount.negate());
            target.changeBalance(amount);
            return;
        }
        source.changeBalance(amount.negate());
        target.changeBalance(amount);
    }

    private void reverseTransferBalances(Account source, Account target, BigDecimal amount) {
        if (source.getKind() == AccountKind.LOAN) {
            if (target.getKind() != AccountKind.ASSET) {
                throw new CustomException(AssetErrorCode.INVALID_LOAN_TRANSFER);
            }
            source.reverseDisbursement(amount);
            target.changeBalance(amount.negate());
            return;
        }
        if (target.getKind() == AccountKind.LOAN) {
            if (source.getKind() != AccountKind.ASSET) {
                throw new CustomException(AssetErrorCode.INVALID_LOAN_TRANSFER);
            }
            source.changeBalance(amount);
            target.changeBalance(amount.negate());
            return;
        }
        source.changeBalance(amount);
        target.changeBalance(amount.negate());
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

    private void publishGoalAchievedCheck(Long userId, Long accountId) {
        eventPublisher.publishEvent(new GoalAchievedCheckEvent(userId, accountId));
    }

    private void publishGoalChecksAfterReverse(Transaction transaction) {
        Account source = transaction.getAccount();
        publishGoalAchievedCheck(transaction.getUser().getId(), source.getId());

        if (transaction.getType() == TransactionType.TRANSFER && transaction.getTargetAccount() != null) {
            Account target = transaction.getTargetAccount();
            publishGoalAchievedCheck(target.getUser().getId(), target.getId());
        }
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
