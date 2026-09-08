package com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response;

import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.GroupPurchase;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.PurchaseStatus;
import java.time.LocalDateTime;

public record GroupPurchaseResponse(
        Long id,
        Long creatorId,
        String creatorNickname,
        Long categoryId,
        String title,
        String content,
        long price,
        int minParticipants,
        int maxParticipants,
        int currentParticipants,
        PurchaseStatus status,
        LocalDateTime deadline,
        String pickupLocation,
        int viewCount,
        String imageUrl,
        double achievementRate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static GroupPurchaseResponse from(GroupPurchase groupPurchase) {
        return of(groupPurchase, "탈퇴한 사용자");
    }

    public static GroupPurchaseResponse of(GroupPurchase groupPurchase, String creatorNickname) {
        double rate = groupPurchase.getMaxParticipants() == 0 ? 0.0 :
                ((double) groupPurchase.getCurrentParticipants() / groupPurchase.getMaxParticipants()) * 100.0;
        rate = Math.round(rate * 100.0) / 100.0;

        return new GroupPurchaseResponse(
                groupPurchase.getId(),
                groupPurchase.getCreatorId(),
                creatorNickname,
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
                groupPurchase.getImageUrl(),
                rate,
                groupPurchase.getCreatedAt(),
                groupPurchase.getUpdatedAt()
        );
    }
}
