package com.chaewookim.accountbookformoms.domain.budget.dto.response;

import java.math.BigDecimal;

public record PublicMonthlyBudgetResponse(
        Long userId,
        String username,
        String yearMonth,
        BigDecimal totalBudget
) {
}
