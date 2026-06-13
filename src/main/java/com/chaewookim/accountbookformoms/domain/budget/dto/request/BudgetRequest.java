package com.chaewookim.accountbookformoms.domain.budget.dto.request;

import java.math.BigDecimal;

public record BudgetRequest(

        Long categoryId,
        String yearMonth,
        BigDecimal totalBudget,
        BigDecimal expectedExpense
) {
}
