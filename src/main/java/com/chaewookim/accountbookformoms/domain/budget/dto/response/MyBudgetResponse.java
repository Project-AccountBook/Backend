package com.chaewookim.accountbookformoms.domain.budget.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record MyBudgetResponse(
        String yearMonth,
        BigDecimal totalBudget,
        List<CategoryBudgetResponse> categoryBudgets
) {
}
