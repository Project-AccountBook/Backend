package com.chaewookim.accountbookformoms.domain.budget.dto.response;

import java.math.BigDecimal;

public record BudgetSummaryResponse(

        String yearMonth,
        BigDecimal totalPlannedBudgetSum,
        BigDecimal totalActualExpenseSum,
        BigDecimal totalRemainingBudget
) {
}
