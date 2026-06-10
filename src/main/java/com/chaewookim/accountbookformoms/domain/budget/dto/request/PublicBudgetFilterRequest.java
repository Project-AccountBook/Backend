package com.chaewookim.accountbookformoms.domain.budget.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record PublicBudgetFilterRequest(

        @Min(1900) @Max(2100)
        Integer yearFrom,

        @Min(1900) @Max(2100)
        Integer yearTo,

        @Min(1) @Max(12)
        Integer monthFrom,

        @Min(1) @Max(12)
        Integer monthTo,

        BigDecimal minAmount,
        BigDecimal maxAmount
) {
}
