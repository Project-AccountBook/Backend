package com.chaewookim.accountbookformoms.domain.income.dto.response;

import java.math.BigDecimal;

public record PublicMonthlyIncomeResponse(
        Long userId,
        String username,
        String yearMonth,
        BigDecimal totalIncome,
        BigDecimal fixedIncome,
        BigDecimal variableIncome
) {
}
