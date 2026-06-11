package com.chaewookim.accountbookformoms.domain.portfolio.dto.response;

import java.math.BigDecimal;

public record PublicMonthlyPortfolioResponse(
        Long userId,
        String username,
        String yearMonth,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal totalBudget,
        BigDecimal balance
) {
    public static PublicMonthlyPortfolioResponse of(Long userId, String username, String yearMonth,
                                                    BigDecimal totalIncome,
                                                    BigDecimal totalExpense,
                                                    BigDecimal totalBudget) {
        return new PublicMonthlyPortfolioResponse(
                userId, username, yearMonth,
                totalIncome, totalExpense, totalBudget,
                totalIncome.subtract(totalExpense));
    }
}
