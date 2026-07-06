package com.chaewookim.accountbookformoms.domain.asset.dto.response;

import com.chaewookim.accountbookformoms.domain.asset.entity.Transaction;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(

        Long id,
        Long accountId,
        String accountName,
        Long targetAccountId,
        String targetAccountName,
        Long categoryId,
        String categoryName,
        TransactionType type,
        BigDecimal amount,
        LocalDate transactionDate,
        String description
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccount().getId(),
                transaction.getAccount().getAccountName(),
                transaction.getTargetAccount() != null ? transaction.getTargetAccount().getId() : null,
                transaction.getTargetAccount() != null ? transaction.getTargetAccount().getAccountName() : null,
                transaction.getTransactionCategory().getId(),
                transaction.getTransactionCategory().getName(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getTransactionDate(),
                transaction.getDescription()
        );
    }
}