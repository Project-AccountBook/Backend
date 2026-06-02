package com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response;

import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.GroupPurchase;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.PurchaseStatus;
import java.time.LocalDateTime;

public record GroupPurchaseResponse(
        Long id,
        Long creatorId,
        Long categoryId,
        String title,
        String content,
        int price,
        int minParticipants,
        int maxParticipants,
        int currentParticipants,
        PurchaseStatus status,
        LocalDateTime deadline,
        String pickupLocation,
        int viewCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static GroupPurchaseResponse from(GroupPurchase groupPurchase) {
        return new GroupPurchaseResponse(
                groupPurchase.getId(),
                groupPurchase.getCreatorId(),
                groupPurchase.getCategoryId(),
                groupPurchase.getTitle(),
                groupPurchase.getContent(),
                groupPurchase.getPrice(),
                groupPurchase.getMinParticipants(),
                groupPurchase.getMaxParticipants(),
                groupPurchase.getCurrentParticipants(),
                groupPurchase.getStatus(),
                groupPurchase.getDeadline(),
                groupPurchase.getPickupLocation(),
                groupPurchase.getViewCount(),
                groupPurchase.getCreatedAt(),
                groupPurchase.getUpdatedAt()
        );
    }
}
