package com.chaewookim.accountbookformoms.domain.budget.dto.response;

import com.chaewookim.accountbookformoms.domain.budget.entity.Budget;

import java.math.BigDecimal;

public record CategoryBudgetResponse(
        Long categoryId,
        String categoryName,
        BigDecimal totalBudget,
        BigDecimal expectedExpense
) {
    public static CategoryBudgetResponse from(Budget budget) {
        Long categoryId = budget.getTransactionCategory() != null ? budget.getTransactionCategory().getId() : null;
        String categoryName = budget.getTransactionCategory() != null ? budget.getTransactionCategory().getName() : null;
        return new CategoryBudgetResponse(categoryId, categoryName, budget.getTotalBudget(), budget.getExpectedExpense());
    }
}
