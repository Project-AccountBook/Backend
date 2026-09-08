package com.chaewookim.accountbookformoms.domain.income.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record UserIncomeDetailResponse(
        Long userId,
        String username,
        String yearMonth,
        BigDecimal totalIncome,
        BigDecimal fixedIncome,
        BigDecimal variableIncome,
        List<CategoryIncomeResponse> fixedCategoryIncomes,
        List<CategoryIncomeResponse> variableCategoryIncomes
) {
}
