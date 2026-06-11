package com.chaewookim.accountbookformoms.domain.expense.dto.response;

import com.chaewookim.accountbookformoms.domain.expense.enums.ExpenseCompareType;

import java.math.BigDecimal;

public record ExpenseCompareResponse(
        ExpenseCompareType type,
        String yearMonth,
        BigDecimal myAmount,
        BigDecimal myFixedAmount,
        BigDecimal myVariableAmount,
        BigDecimal averageAmount,
        BigDecimal averageFixedAmount,
        BigDecimal averageVariableAmount,
        long sampleSize,
        BigDecimal difference
) {
    public static ExpenseCompareResponse of(ExpenseCompareType type, String yearMonth,
                                            BigDecimal myFixed, BigDecimal myVariable,
                                            BigDecimal averageFixed, BigDecimal averageVariable,
                                            long sampleSize) {
        BigDecimal myAmount = myFixed.add(myVariable);
        BigDecimal averageAmount = averageFixed.add(averageVariable);
        return new ExpenseCompareResponse(
                type, yearMonth,
                myAmount, myFixed, myVariable,
                averageAmount, averageFixed, averageVariable,
                sampleSize,
                myAmount.subtract(averageAmount));
    }
}
