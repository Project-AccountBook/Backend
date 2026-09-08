package com.chaewookim.accountbookformoms.domain.asset.dto.request;

import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionFrequency;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.*;

public record FixedTransactionRequest(

        @NotNull(message = "계좌 ID는 필수입니다.")
        Long accountId,

        Long targetAccountId,

        @NotNull(message = "카테고리 ID는 필수입니다.")
        Long categoryId,

        @NotNull(message = "거래 유형은 필수입니다.")
        TransactionType type,

        @NotNull(message = "금액은 필수입니다.")
        @Positive(message = "금액은 0보다 커야 합니다.")
        BigDecimal amount,

        @NotNull(message = "반복 주기는 필수입니다.")
        TransactionFrequency frequency,

        @NotNull(message = "반복일은 필수입니다.")
        Integer repeatDay,

        Integer repeatMonth,

        @NotNull(message = "시작일은 필수입니다.")
        LocalDate startDate,

        LocalDate endDate,

        String description
) {
}
