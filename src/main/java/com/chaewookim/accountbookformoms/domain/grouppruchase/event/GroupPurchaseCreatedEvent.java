package com.chaewookim.accountbookformoms.domain.grouppruchase.event;

public record GroupPurchaseCreatedEvent(

        String categoryName,
        String title,
        Long groupPurchaseId
) {
}
