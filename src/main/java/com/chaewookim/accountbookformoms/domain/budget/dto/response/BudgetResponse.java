package com.chaewookim.accountbookformoms.domain.budget.dto.response;

import java.math.BigDecimal;

public record BudgetResponse(

        Long id,
        Long categoryId,
        String categoryName,
        boolean categoryArchived,
        BigDecimal totalBudget,
        BigDecimal expectedExpense,
        BigDecimal totalPlannedBudget,
        BigDecimal actualExpense,
        BigDecimal remainingBudget,
        BigDecimal progress
) {
}
