package com.chaewookim.accountbookformoms.domain.grouppruchase.event;

public record GroupPurchaseCreatedEvent(

        Long categoryId,
        String categoryName,
        String title,
        Long groupPurchaseId
) {
}
