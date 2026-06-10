package com.chaewookim.accountbookformoms.domain.expense.dto.response;

public record PairExpenseDetailResponse(
        UserExpenseDetailResponse me,
        UserExpenseDetailResponse target
) {
}
