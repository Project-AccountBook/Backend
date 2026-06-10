package com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response;

import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.Category;

import java.time.LocalDateTime;

public record GroupPurchaseCategoryResponse(
        Long id,
        String name,
        int sortOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static GroupPurchaseCategoryResponse from(Category category) {
        return new GroupPurchaseCategoryResponse(
                category.getId(),
                category.getName(),
                category.getSortOrder(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
