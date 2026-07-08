package com.chaewookim.accountbookformoms.domain.asset.application;

import com.chaewookim.accountbookformoms.domain.asset.dto.request.TransactionRequest;
import com.chaewookim.accountbookformoms.domain.asset.entity.FixedTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class FixedTransactionExecutor {

    private final TransactionService transactionService;

    public boolean executeIfDue(FixedTransaction fixedTransaction, LocalDate today) {
        LocalDate executionDate = resolveExecutionDate(fixedTransaction, today);
        if (executionDate == null) {
            return false;
        }

        TransactionRequest request = new TransactionRequest(
                fixedTransaction.getAccount().getId(),
                null,
                fixedTransaction.getTransactionCategory().getId(),
                fixedTransaction.getType(),
                fixedTransaction.getAmount(),
                executionDate,
                fixedTransaction.getDescription()
        );

        transactionService.createTransactionFromFixed(fixedTransaction.getUser().getId(), request);
        fixedTransaction.updateExecutionStatus(executionDate);
        log.info("고정 거래 생성 완료: ID={}", fixedTransaction.getId());
        return true;
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
