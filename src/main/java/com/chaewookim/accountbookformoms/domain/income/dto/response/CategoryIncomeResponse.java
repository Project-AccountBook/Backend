package com.chaewookim.accountbookformoms.domain.income.dto.response;

import java.math.BigDecimal;

public record CategoryIncomeResponse(
        Long categoryId,
        String categoryName,
        BigDecimal amount
) {
}
