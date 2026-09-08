package com.chaewookim.accountbookformoms.domain.budget.dto.response;

public record PairBudgetDetailResponse(
        UserBudgetDetailResponse me,
        UserBudgetDetailResponse target
) {
}
