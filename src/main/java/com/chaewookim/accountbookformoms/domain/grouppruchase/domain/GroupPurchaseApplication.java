package com.chaewookim.accountbookformoms.domain.grouppruchase.domain;

import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.ApplicationStatus;
import com.chaewookim.accountbookformoms.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE group_purchase_application SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class GroupPurchaseApplication extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;          // 신청자 유저ID

    @Column(nullable = false)
    private String title;          // 공구 상품명/제목

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;        // 상세 설명/신청 사유

    @Column(nullable = true)
    private String productUrl;     // 상품 링크 (선택)

    @Column(nullable = false)
    private int expectedPrice;     // 희망 인당 금액

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ApplicationStatus status; // 신청 상태

    @Column(nullable = true)
    private String adminFeedback;  // 관리자 피드백 의견

    @Builder
    public GroupPurchaseApplication(Long id, Long userId, String title, String content, String productUrl, int expectedPrice) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.productUrl = productUrl;
        this.expectedPrice = expectedPrice;
        this.status = ApplicationStatus.PENDING;
    }

    public void updateStatus(ApplicationStatus status, String adminFeedback) {
        this.status = status;
        this.adminFeedback = adminFeedback;
    }
}
