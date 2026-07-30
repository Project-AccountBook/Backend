package com.chaewookim.accountbookformoms.domain.budget.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record BudgetCopyResponse(
        String sourceYearMonth,
        String targetYearMonth,
        List<Item> items,
        int copyCount
) {
    public record Item(
            Long categoryId,
            String categoryName,
            BigDecimal totalBudget,
            BigDecimal expectedExpense,
            boolean selected,
            String skipReason
    ) {
    }
}
