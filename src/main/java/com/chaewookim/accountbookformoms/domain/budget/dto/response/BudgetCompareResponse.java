package com.chaewookim.accountbookformoms.domain.budget.dto.response;

import com.chaewookim.accountbookformoms.domain.budget.enums.BudgetCompareType;

import java.math.BigDecimal;

public record BudgetCompareResponse(
        BudgetCompareType type,
        String yearMonth,
        BigDecimal myAmount,
        BigDecimal averageAmount,
        long sampleSize,
        BigDecimal difference
) {
    public static BudgetCompareResponse of(BudgetCompareType type, String yearMonth,
                                           BigDecimal myAmount, BigDecimal averageAmount, long sampleSize) {
        BigDecimal diff = myAmount.subtract(averageAmount);
        return new BudgetCompareResponse(type, yearMonth, myAmount, averageAmount, sampleSize, diff);
    }
}
