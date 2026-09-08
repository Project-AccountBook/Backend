package com.chaewookim.accountbookformoms.domain.expense.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record UserExpenseDetailResponse(
        Long userId,
        String username,
        String yearMonth,
        BigDecimal totalExpense,
        BigDecimal fixedExpense,
        BigDecimal variableExpense,
        List<CategoryExpenseResponse> fixedCategoryExpenses,
        List<CategoryExpenseResponse> variableCategoryExpenses
) {
}
