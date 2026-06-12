package com.chaewookim.accountbookformoms.domain.income.dto.request;

import com.chaewookim.accountbookformoms.domain.income.enums.IncomeCompareType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record IncomeCompareRequest(

        @NotNull(message = "비교 기준은 필수입니다.")
        IncomeCompareType type,

        @NotBlank(message = "연/월은 필수입니다.")
        @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "연/월은 YYYY-MM 형식이어야 합니다.")
        String yearMonth,

        BigDecimal minAmount,
        BigDecimal maxAmount,
        Long categoryId,

        @DecimalMin(value = "0.1", message = "반경은 0.1km 이상이어야 합니다.")
        @DecimalMax(value = "50.0", message = "반경은 50km 이하여야 합니다.")
        Double radiusKm
) {
}
