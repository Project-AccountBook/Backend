package com.chaewookim.accountbookformoms.domain.asset.application;

import com.chaewookim.accountbookformoms.domain.asset.dto.request.TransactionRequest;
import com.chaewookim.accountbookformoms.domain.asset.entity.FixedTransaction;
import com.chaewookim.accountbookformoms.domain.asset.enums.FixedTransactionExecutionFailure;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
import com.chaewookim.accountbookformoms.domain.asset.error.AssetErrorCode;
import com.chaewookim.accountbookformoms.domain.asset.event.FixedTransactionExecutionFailedEvent;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class FixedTransactionExecutor {

    private final TransactionService transactionService;
    private final ApplicationEventPublisher eventPublisher;

    public boolean executeIfDue(FixedTransaction fixedTransaction, LocalDate today) {
        if (fixedTransaction.hasExecutionFailure()) {
            return false;
        }

        LocalDate executionDate = resolveExecutionDate(fixedTransaction, today);
        if (executionDate == null) {
            return false;
        }

        return executeOccurrence(fixedTransaction, executionDate, true);
    }

    public void retryFailedOccurrence(FixedTransaction fixedTransaction) {
        if (!fixedTransaction.hasExecutionFailure()) {
            throw new CustomException(AssetErrorCode.FIXED_TRANSACTION_NOT_FAILED);
        }
        executeOccurrence(fixedTransaction, fixedTransaction.getFailedExecutionDate(), false);
    }

    private boolean executeOccurrence(FixedTransaction fixedTransaction, LocalDate executionDate, boolean notifyOnBalanceFailure) {
        TransactionRequest request = new TransactionRequest(
                fixedTransaction.getAccount().getId(),
                fixedTransaction.getTargetAccount() != null ? fixedTransaction.getTargetAccount().getId() : null,
                fixedTransaction.getTransactionCategory().getId(),
                fixedTransaction.getType(),
                fixedTransaction.getAmount(),
                executionDate,
                fixedTransaction.getDescription()
        );

        try {
            transactionService.createTransactionFromFixed(fixedTransaction.getUser().getId(), request);
            fixedTransaction.updateExecutionStatus(executionDate);
            log.info("고정 거래 생성 완료: ID={}", fixedTransaction.getId());
            return true;
        } catch (CustomException e) {
            FixedTransactionExecutionFailure failure = resolveBalanceFailure(fixedTransaction.getType(), e);
            if (failure == null) {
                throw e;
            }

            boolean alreadyFailed = fixedTransaction.hasExecutionFailure();
            fixedTransaction.markExecutionFailed(executionDate, failure);
            log.warn("고정 거래 잔액/한도 부족: ID={}, reason={}", fixedTransaction.getId(), failure);

            if (notifyOnBalanceFailure && !alreadyFailed) {
                eventPublisher.publishEvent(new FixedTransactionExecutionFailedEvent(
                        fixedTransaction.getUser().getId(),
                        fixedTransaction.getId(),
                        fixedTransaction.getAccount().getAccountName(),
                        fixedTransaction.getDescription(),
                        failure
                ));
            }

            if (!notifyOnBalanceFailure) {
                throw e;
            }
            return false;
        }
    }

    private FixedTransactionExecutionFailure resolveBalanceFailure(TransactionType type, CustomException e) {
        if (type != TransactionType.EXPENSE && type != TransactionType.TRANSFER) {
            return null;
        }
        if (e.getErrorCode() == AssetErrorCode.INSUFFICIENT_BALANCE) {
            return FixedTransactionExecutionFailure.INSUFFICIENT_BALANCE;
        }
        if (e.getErrorCode() == AssetErrorCode.CREDIT_LIMIT_EXCEEDED) {
            return FixedTransactionExecutionFailure.CREDIT_LIMIT_EXCEEDED;
        }
        return null;
    }

    private LocalDate resolveExecutionDate(FixedTransaction fixedTransaction, LocalDate today) {
        if (fixedTransaction.isExecutionDay(today) && !today.equals(fixedTransaction.getLastExecutedDate())) {
            return today;
        }

        LocalDate nextExecutionDate = fixedTransaction.getNextExecutionDate();
        if (nextExecutionDate != null
                && !nextExecutionDate.isAfter(today)
                && !nextExecutionDate.equals(fixedTransaction.getLastExecutedDate())
                && fixedTransaction.isExecutionDay(nextExecutionDate)) {
            return nextExecutionDate;
        }

        return null;
    }
}
