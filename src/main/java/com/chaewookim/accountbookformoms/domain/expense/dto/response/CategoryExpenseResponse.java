package com.chaewookim.accountbookformoms.domain.expense.dto.response;

import java.math.BigDecimal;

public record CategoryExpenseResponse(
        Long categoryId,
        String categoryName,
        BigDecimal amount
) {
}
