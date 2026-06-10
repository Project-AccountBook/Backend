package com.chaewookim.accountbookformoms.domain.budget.dto.request;

import com.chaewookim.accountbookformoms.domain.budget.enums.BudgetCompareType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record BudgetCompareRequest(

        @NotNull(message = "비교 기준은 필수입니다.")
        BudgetCompareType type,

        @NotBlank(message = "연/월은 필수입니다.")
        @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "연/월은 YYYY-MM 형식이어야 합니다.")
        String yearMonth,

        BigDecimal minAmount,
        BigDecimal maxAmount,
        Long categoryId
) {
}
