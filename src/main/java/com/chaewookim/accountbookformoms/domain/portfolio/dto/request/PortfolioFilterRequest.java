package com.chaewookim.accountbookformoms.domain.portfolio.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record PortfolioFilterRequest(

        @Min(1900) @Max(2100)
        Integer year,

        @Min(1) @Max(12)
        Integer month,

        BigDecimal minIncome,
        BigDecimal maxIncome,
        BigDecimal minExpense,
        BigDecimal maxExpense,
        BigDecimal minBudget,
        BigDecimal maxBudget
) {
}
