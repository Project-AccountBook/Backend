package com.chaewookim.accountbookformoms.domain.income.dto.response;

import com.chaewookim.accountbookformoms.domain.income.enums.IncomeCompareType;

import java.math.BigDecimal;

public record IncomeCompareResponse(
        IncomeCompareType type,
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
    public static IncomeCompareResponse of(IncomeCompareType type, String yearMonth,
                                           BigDecimal myFixed, BigDecimal myVariable,
                                           BigDecimal averageFixed, BigDecimal averageVariable,
                                           long sampleSize) {
        BigDecimal myAmount = myFixed.add(myVariable);
        BigDecimal averageAmount = averageFixed.add(averageVariable);
        return new IncomeCompareResponse(
                type, yearMonth,
                myAmount, myFixed, myVariable,
                averageAmount, averageFixed, averageVariable,
                sampleSize,
                myAmount.subtract(averageAmount));
    }
}
