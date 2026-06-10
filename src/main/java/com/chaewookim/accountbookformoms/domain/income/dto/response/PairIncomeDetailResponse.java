package com.chaewookim.accountbookformoms.domain.income.dto.response;

public record PairIncomeDetailResponse(
        UserIncomeDetailResponse me,
        UserIncomeDetailResponse target
) {
}
