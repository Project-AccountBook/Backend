package com.chaewookim.accountbookformoms.domain.income.dto.response;

import com.chaewookim.accountbookformoms.domain.income.enums.IncomeCompareType;

import java.math.BigDecimal;

public record UserIncomeCompareResponse(
        IncomeCompareType type,
        String yearMonth,
        Long myUserId,
        String myLabel,
        BigDecimal myAmount,
        BigDecimal myFixedAmount,
        BigDecimal myVariableAmount,
        Long targetUserId,
        String targetLabel,
        BigDecimal targetAmount,
        BigDecimal targetFixedAmount,
        BigDecimal targetVariableAmount,
        BigDecimal difference
) {
    public static UserIncomeCompareResponse of(IncomeCompareType type, String yearMonth,
                                               Long myUserId, String myLabel,
                                               BigDecimal myFixed, BigDecimal myVariable,
                                               Long targetUserId, String targetLabel,
                                               BigDecimal targetFixed, BigDecimal targetVariable) {
        BigDecimal myAmount = myFixed.add(myVariable);
        BigDecimal targetAmount = targetFixed.add(targetVariable);
        return new UserIncomeCompareResponse(
                type, yearMonth,
                myUserId, myLabel, myAmount, myFixed, myVariable,
                targetUserId, targetLabel, targetAmount, targetFixed, targetVariable,
                myAmount.subtract(targetAmount));
    }
}
