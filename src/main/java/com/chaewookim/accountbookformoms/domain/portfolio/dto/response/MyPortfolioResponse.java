package com.chaewookim.accountbookformoms.domain.portfolio.dto.response;

import com.chaewookim.accountbookformoms.domain.budget.dto.response.MyBudgetResponse;
import com.chaewookim.accountbookformoms.domain.expense.dto.response.MyExpenseResponse;
import com.chaewookim.accountbookformoms.domain.income.dto.response.MyIncomeResponse;

import java.math.BigDecimal;

public record MyPortfolioResponse(
        String yearMonth,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal totalBudget,
        BigDecimal balance,
        MyIncomeResponse income,
        MyExpenseResponse expense,
        MyBudgetResponse budget
) {
    public static MyPortfolioResponse of(String yearMonth,
                                         MyIncomeResponse income,
                                         MyExpenseResponse expense,
                                         MyBudgetResponse budget) {
        BigDecimal totalIncome = income.totalIncome();
        BigDecimal totalExpense = expense.totalExpense();
        BigDecimal totalBudget = budget.totalBudget();
        return new MyPortfolioResponse(
                yearMonth,
                totalIncome,
                totalExpense,
                totalBudget,
                totalIncome.subtract(totalExpense),
                income, expense, budget);
    }
}
