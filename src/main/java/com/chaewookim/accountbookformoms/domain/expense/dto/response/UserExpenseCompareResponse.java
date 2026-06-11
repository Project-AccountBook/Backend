package com.chaewookim.accountbookformoms.domain.expense.dto.response;

import com.chaewookim.accountbookformoms.domain.expense.enums.ExpenseCompareType;

import java.math.BigDecimal;

public record UserExpenseCompareResponse(
        ExpenseCompareType type,
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
    public static UserExpenseCompareResponse of(ExpenseCompareType type, String yearMonth,
                                                Long myUserId, String myLabel,
                                                BigDecimal myFixed, BigDecimal myVariable,
                                                Long targetUserId, String targetLabel,
                                                BigDecimal targetFixed, BigDecimal targetVariable) {
        BigDecimal myAmount = myFixed.add(myVariable);
        BigDecimal targetAmount = targetFixed.add(targetVariable);
        return new UserExpenseCompareResponse(
                type, yearMonth,
                myUserId, myLabel, myAmount, myFixed, myVariable,
                targetUserId, targetLabel, targetAmount, targetFixed, targetVariable,
                myAmount.subtract(targetAmount));
    }
}
