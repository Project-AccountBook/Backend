package com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response;

import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.GroupPurchase;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.PurchaseStatus;

import java.time.LocalDateTime;

public record GroupPurchaseAdminResponse(
        Long id,
        Long creatorId,
        String creatorUsername,
        Long categoryId,
        String categoryName,
        String title,
        int price,
        int minParticipants,
        int maxParticipants,
        int currentParticipants,
        PurchaseStatus status,
        LocalDateTime deadline,
        LocalDateTime createdAt,
        long reportCount
) {
    public static GroupPurchaseAdminResponse of(GroupPurchase gp, String creatorUsername, String categoryName, long reportCount) {
        return new GroupPurchaseAdminResponse(
                gp.getId(),
                gp.getCreatorId(),
                creatorUsername,
                gp.getCategoryId(),
                categoryName,
                gp.getTitle(),
                gp.getPrice(),
                gp.getMinParticipants(),
                gp.getMaxParticipants(),
                gp.getCurrentParticipants(),
                gp.getStatus(),
                gp.getDeadline(),
                gp.getCreatedAt(),
                reportCount
        );
    }
}
