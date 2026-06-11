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
import com.chaewookim.accountbookformoms.global.error.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionCategoryRepository categoryRepository;


    @Transactional
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

        return saveTransaction(source, source, request);
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
                .transactionCategory(category)
                .type(request.type())
                .amount(request.amount())
                .transactionDate(request.transactionDate())
                .description(request.description())
                .build();

        return transactionRepository.save(transaction).getId();
    }

    public List<TransactionResponse> getTransactions(Long userId, Long accountId, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.findAllByUserIdAndAccountIdAndTransactionDateBetween(userId, accountId, startDate, endDate)
                .stream().map(TransactionResponse::from).toList();
    }

    public TransactionResponse getTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new CustomException(AssetErrorCode.TRANSACTION_NOT_FOUND));
        return TransactionResponse.from(transaction);
    }

    @Transactional
    public void updateTransaction(Long userId, Long transactionId, TransactionRequest request) {

        Transaction transaction = validateAndGet(userId, transactionId);

        Account account = accountRepository.findByIdWithLock(transaction.getAccount().getId())
                .orElseThrow(() -> new CustomException(AssetErrorCode.ACCOUNT_NOT_FOUND));

        TransactionCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new CustomException(AssetErrorCode.CATEGORY_NOT_FOUND));

        account.changeBalance(transaction.getBalanceChangeAmount().negate());
        transaction.update(request, category);
        account.changeBalance(transaction.getBalanceChangeAmount());
    }

    @Transactional
    public void deleteTransaction(Long userId, Long transactionId) {

        Transaction transaction = validateAndGet(userId, transactionId);

        Account account = accountRepository.findByIdWithLock(transaction.getAccount().getId())
                .orElseThrow(() -> new CustomException(AssetErrorCode.ACCOUNT_NOT_FOUND));

        account.changeBalance(transaction.getBalanceChangeAmount().negate());
        transactionRepository.delete(transaction);
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
}
