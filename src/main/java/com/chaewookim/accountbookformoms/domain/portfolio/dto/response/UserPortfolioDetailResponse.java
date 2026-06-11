package com.chaewookim.accountbookformoms.domain.portfolio.dto.response;

import com.chaewookim.accountbookformoms.domain.budget.dto.response.UserBudgetDetailResponse;
import com.chaewookim.accountbookformoms.domain.expense.dto.response.UserExpenseDetailResponse;
import com.chaewookim.accountbookformoms.domain.income.dto.response.UserIncomeDetailResponse;

import java.math.BigDecimal;

public record UserPortfolioDetailResponse(
        Long userId,
        String username,
        String yearMonth,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal totalBudget,
        BigDecimal balance,
        UserIncomeDetailResponse income,
        UserExpenseDetailResponse expense,
        UserBudgetDetailResponse budget
) {
    public static UserPortfolioDetailResponse of(UserIncomeDetailResponse income,
                                                 UserExpenseDetailResponse expense,
                                                 UserBudgetDetailResponse budget) {
        BigDecimal totalIncome = income.totalIncome();
        BigDecimal totalExpense = expense.totalExpense();
        BigDecimal totalBudget = budget.totalBudget();
        return new UserPortfolioDetailResponse(
                income.userId(),
                income.username(),
                income.yearMonth(),
                totalIncome,
                totalExpense,
                totalBudget,
                totalIncome.subtract(totalExpense),
                income, expense, budget);
    }
}
