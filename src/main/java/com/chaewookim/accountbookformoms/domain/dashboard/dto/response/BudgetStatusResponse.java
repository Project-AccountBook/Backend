package com.chaewookim.accountbookformoms.domain.dashboard.dto.response;

import com.chaewookim.accountbookformoms.domain.budget.dto.response.BudgetSummaryResponse;

import java.math.BigDecimal;

public record BudgetStatusResponse(

        BigDecimal totalPlanned,
        BigDecimal actualExpense,
        BigDecimal remaining
) {
    public BudgetStatusResponse(BudgetSummaryResponse b) {
        this(
                b.totalPlannedBudgetSum(),
                b.totalActualExpenseSum(),
                b.totalRemainingBudget()
        );
    }
}
