package com.chaewookim.accountbookformoms.domain.asset.dto.request;

import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(

        @NotNull(message = "계좌 ID는 필수입니다.")
        Long accountId,

        Long targetAccountId, // 이체 시 사용할 대상 계좌 ID

        @NotNull(message = "카테고리 ID는 필수입니다.")
        Long categoryId,

        @NotNull(message = "거래 유형은 필수입니다.")
        TransactionType type,

        @NotNull(message = "금액은 필수입니다.")
        @Positive(message = "금액은 0보다 커야 합니다.")
        BigDecimal amount,

        @NotNull(message = "거래일은 필수입니다.")
        LocalDate transactionDate,

        String description
) {
}
