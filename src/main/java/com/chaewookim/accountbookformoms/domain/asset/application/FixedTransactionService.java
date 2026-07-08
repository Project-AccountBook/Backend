package com.chaewookim.accountbookformoms.domain.asset.application;

import com.chaewookim.accountbookformoms.domain.asset.dao.AccountRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.FixedTransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionCategoryRepository;
import com.chaewookim.accountbookformoms.domain.asset.dto.request.FixedTransactionRequest;
import com.chaewookim.accountbookformoms.domain.asset.dto.response.FixedTransactionResponse;
import com.chaewookim.accountbookformoms.domain.asset.entity.Account;
import com.chaewookim.accountbookformoms.domain.asset.entity.FixedTransaction;
import com.chaewookim.accountbookformoms.domain.asset.entity.TransactionCategory;
import com.chaewookim.accountbookformoms.domain.asset.error.AssetErrorCode;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FixedTransactionService {

    private final FixedTransactionRepository fixedTransactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionCategoryRepository categoryRepository;
    private final FixedTransactionImmediateExecutionService fixedTransactionImmediateExecutionService;

    @Transactional
    public Long createFixedTransaction(Long userId, User user, FixedTransactionRequest request) {

        Account account = accountRepository.findByIdAndUserId(request.accountId(), userId)
                .orElseThrow(() -> new CustomException(AssetErrorCode.ACCOUNT_NOT_FOUND));
        TransactionCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new CustomException(AssetErrorCode.CATEGORY_NOT_FOUND));

        FixedTransaction fixedTransaction = FixedTransaction.builder()
                .user(user)
                .account(account)
                .transactionCategory(category)
                .type(request.type())
                .amount(request.amount())
                .frequency(request.frequency())
                .repeatDay(request.repeatDay())
                .repeatMonth(request.repeatMonth())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .description(request.description())
                .build();

        FixedTransaction saved = fixedTransactionRepository.save(fixedTransaction);
        scheduleImmediateExecution(saved.getId());
        return saved.getId();
    }

    private void scheduleImmediateExecution(Long fixedTransactionId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            fixedTransactionImmediateExecutionService.executeIfDue(fixedTransactionId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                fixedTransactionImmediateExecutionService.executeIfDue(fixedTransactionId);
            }
        });
    }

    @Transactional
    public List<FixedTransactionResponse> getFixedTransactions(Long userId) {
        List<FixedTransactionResponse> responses = new java.util.ArrayList<>();

        for (FixedTransaction fixedTransaction : fixedTransactionRepository.findAllByUserId(userId)) {
            if (!hasActiveAccount(fixedTransaction)) {
                fixedTransactionRepository.delete(fixedTransaction);
                continue;
            }
            responses.add(FixedTransactionResponse.from(fixedTransaction));
        }

        return responses;
    }

    private boolean hasActiveAccount(FixedTransaction fixedTransaction) {
        Account account = fixedTransaction.getAccount();
        if (account == null) {
            return false;
        }
        return accountRepository.findById(account.getId()).isPresent();
    }

    @Transactional
    public void updateFixedTransaction(Long userId, Long id, FixedTransactionRequest request) {

        FixedTransaction fixedTransaction = validateAndGet(userId, id);

        Account account = accountRepository.findByIdAndUserId(request.accountId(), userId)
                .orElseThrow(() -> new CustomException(AssetErrorCode.ACCOUNT_NOT_FOUND));
        TransactionCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new CustomException(AssetErrorCode.CATEGORY_NOT_FOUND));

        fixedTransaction.update(account, category, request);
    }

    @Transactional
    public void toggleActiveStatus(Long userId, Long id) {
        FixedTransaction fixedTransaction = validateAndGet(userId, id);
        fixedTransaction.toggleActive();
    }

    @Transactional
    public void deleteFixedTransaction(Long userId, Long id) {
        FixedTransaction fixedTransaction = validateAndGet(userId, id);
        fixedTransactionRepository.delete(fixedTransaction);
    }

    // 공통 검증 로직
    private FixedTransaction validateAndGet(Long userId, Long id) {

        FixedTransaction fixedTransaction = fixedTransactionRepository.findById(id)
                .orElseThrow(() -> new CustomException(AssetErrorCode.FIXED_TRANSACTION_NOT_FOUND));

        if (!fixedTransaction.getUser().getId().equals(userId)) {
            throw new CustomException(AssetErrorCode.FIXED_TRANSACTION_FORBIDDEN);
        }

        return fixedTransaction;
    }
}
