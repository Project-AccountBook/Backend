package com.chaewookim.accountbookformoms.domain.asset.dto.response;

import com.chaewookim.accountbookformoms.domain.asset.entity.Transaction;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(

        Long id,
        Long accountId,
        String accountName,
        boolean accountArchived,
        Long targetAccountId,
        String targetAccountName,
        boolean targetAccountArchived,
        Long categoryId,
        String categoryName,
        TransactionType type,
        BigDecimal amount,
        LocalDate transactionDate,
        String description
) {
    public static TransactionResponse from(Transaction transaction) {
        Long accountId = transaction.getSnapshotAccountId() != null
                ? transaction.getSnapshotAccountId()
                : (transaction.getAccount() != null ? transaction.getAccount().getId() : null);
        String accountName = transaction.getSnapshotAccountName() != null
                ? transaction.getSnapshotAccountName()
                : (transaction.getAccount() != null ? transaction.getAccount().getAccountName() : "삭제된 계좌");

        Long targetAccountId = transaction.getSnapshotTargetAccountId() != null
                ? transaction.getSnapshotTargetAccountId()
                : (transaction.getTargetAccount() != null ? transaction.getTargetAccount().getId() : null);
        String targetAccountName = transaction.getSnapshotTargetAccountName() != null
                ? transaction.getSnapshotTargetAccountName()
                : (transaction.getTargetAccount() != null ? transaction.getTargetAccount().getAccountName() : null);

        return new TransactionResponse(
                transaction.getId(),
                accountId,
                accountName,
                transaction.isAccountArchived(),
                targetAccountId,
                targetAccountName,
                transaction.isTargetAccountArchived(),
                transaction.getTransactionCategory().getId(),
                transaction.getTransactionCategory().getName(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getTransactionDate(),
                transaction.getDescription()
        );
    }
}
