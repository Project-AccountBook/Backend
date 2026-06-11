package com.chaewookim.accountbookformoms.domain.expense.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record PublicExpenseFilterRequest(

        @Min(1900) @Max(2100)
        Integer year,

        @Min(1) @Max(12)
        Integer month,

        BigDecimal minAmount,
        BigDecimal maxAmount
) {
}
