package com.chaewookim.accountbookformoms.domain.grouppruchase.domain;

import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.PurchaseStatus;
import com.chaewookim.accountbookformoms.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE group_purchase SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class GroupPurchase extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long creatorId;          // 개설자 유저ID

    @Column(nullable = false)
    private Long categoryId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;          // 공동 구매 상세 본문

    @Column(nullable = false)
    private int price;               // 인당 금액

    @Column(nullable = false)
    private int minParticipants;     // 최소 성사 인원

    @Column(nullable = false)
    private int maxParticipants;     // 최대 제한 인원

    @Column(nullable = false)
    private int currentParticipants; // 현재 참여 인원

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PurchaseStatus status;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Column(nullable = false)
    private String pickupLocation;   // 수령 장소

    @Column(nullable = false)
    private int viewCount;           // 조회수

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
    }

    public void update(Long categoryId, String title, String content, int price, int minParticipants, int maxParticipants, LocalDateTime deadline, String pickupLocation) {
        this.categoryId = categoryId;
        this.title = title;
        this.content = content;
        this.price = price;
        this.minParticipants = minParticipants;
        this.maxParticipants = maxParticipants;
        this.deadline = deadline;
        this.pickupLocation = pickupLocation;
    }
}