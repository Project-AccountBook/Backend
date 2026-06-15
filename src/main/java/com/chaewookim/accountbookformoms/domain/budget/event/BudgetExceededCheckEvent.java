package com.chaewookim.accountbookformoms.domain.budget.event;

public record BudgetExceededCheckEvent(

        Long userId,
        String yearMonth,
        Long categoryId
) {
}
