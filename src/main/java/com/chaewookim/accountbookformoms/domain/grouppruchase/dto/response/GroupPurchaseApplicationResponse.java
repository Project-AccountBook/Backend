package com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response;

import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.GroupPurchaseApplication;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.ApplicationStatus;

import java.time.LocalDateTime;

public record GroupPurchaseApplicationResponse(
        Long id,
        Long userId,
        String title,
        String content,
        String productUrl,
        int expectedPrice,
        ApplicationStatus status,
        String adminFeedback,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static GroupPurchaseApplicationResponse from(GroupPurchaseApplication application) {
        return new GroupPurchaseApplicationResponse(
                application.getId(),
                application.getUserId(),
                application.getTitle(),
                application.getContent(),
                application.getProductUrl(),
                application.getExpectedPrice(),
                application.getStatus(),
                application.getAdminFeedback(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }
}
