package com.chaewookim.accountbookformoms.domain.budget.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record UserBudgetDetailResponse(
        Long userId,
        String username,
        String yearMonth,
        BigDecimal totalBudget,
        List<CategoryBudgetResponse> categoryBudgets
) {
}
