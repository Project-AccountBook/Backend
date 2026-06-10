package com.chaewookim.accountbookformoms.domain.expense.dto.response;

import java.math.BigDecimal;

public record PublicMonthlyExpenseResponse(
        Long userId,
        String username,
        String yearMonth,
        BigDecimal totalExpense,
        BigDecimal fixedExpense,
        BigDecimal variableExpense
) {
}
