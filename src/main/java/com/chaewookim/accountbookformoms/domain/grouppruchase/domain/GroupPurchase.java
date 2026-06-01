package com.chaewookim.accountbookformoms.domain.grouppruchase.domain;

import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.PurchaseStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupPurchase {

    private Long id;
    private Long creatorId;          // 개설자 유저ID
    private Long categoryId;
    private String title;
    private String content;          // 공동 구매 상세 본문
    private int price;               // 인당 금액
    private int minParticipants;     // 최소 성사 인원
    private int maxParticipants;     // 최대 제한 인원
    private int currentParticipants; // 현재 참여 인원
    private PurchaseStatus status;
    private LocalDateTime deadline;
    private String pickupLocation;   // 수령 장소
    private int viewCount;           // 조회수
    private boolean isDeleted;       // 삭제 여부
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    public GroupPurchase(Long id, Long creatorId
            , Long categoryId, String title
            , String content, int price
            , int minParticipants, int maxParticipants
            , LocalDateTime deadline, String pickupLocation
            , Double latitude, Double longitude)
    {
        this.id = id;
        this.creatorId = creatorId;
        this.categoryId = categoryId;
        this.title = title;
        this.content = content;
        this.price = price;
        this.minParticipants = minParticipants;
        this.maxParticipants = maxParticipants;
        this.currentParticipants = 0;
        this.status = PurchaseStatus.RECRUITING;
        this.deadline = deadline;
        this.pickupLocation = pickupLocation;
        this.viewCount = 0;
        this.isDeleted = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}