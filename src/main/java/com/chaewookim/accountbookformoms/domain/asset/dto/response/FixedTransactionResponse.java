package com.chaewookim.accountbookformoms.domain.asset.dto.response;

import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionFrequency;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
import com.chaewookim.accountbookformoms.domain.asset.entity.FixedTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FixedTransactionResponse(

        Long id,
        String accountName,
        String categoryName,
        TransactionType type,
        BigDecimal amount,
        TransactionFrequency frequency,
        Integer repeatDay,
        LocalDate startDate,
        LocalDate endDate,
        String description,
        Boolean isActive
) {
    public static FixedTransactionResponse from(FixedTransaction entity) {
        return new FixedTransactionResponse(
                entity.getId(),
                entity.getAccount().getAccountName(),
                entity.getTransactionCategory().getName(),
                entity.getType(),
                entity.getAmount(),
                entity.getFrequency(),
                entity.getRepeatDay(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getDescription(),
                entity.getIsActive()
        );
    }
}
