package com.chaewookim.accountbookformoms.domain.budget.dto.response;

import com.chaewookim.accountbookformoms.domain.budget.enums.BudgetCompareType;

import java.math.BigDecimal;

public record UserBudgetCompareResponse(
        BudgetCompareType type,
        String yearMonth,
        Long myUserId,
        String myLabel,
        BigDecimal myAmount,
        Long targetUserId,
        String targetLabel,
        BigDecimal targetAmount,
        BigDecimal difference
) {
    public static UserBudgetCompareResponse of(BudgetCompareType type, String yearMonth,
                                               Long myUserId, String myLabel, BigDecimal myAmount,
                                               Long targetUserId, String targetLabel, BigDecimal targetAmount) {
        return new UserBudgetCompareResponse(
                type, yearMonth,
                myUserId, myLabel, myAmount,
                targetUserId, targetLabel, targetAmount,
                myAmount.subtract(targetAmount));
    }
}
