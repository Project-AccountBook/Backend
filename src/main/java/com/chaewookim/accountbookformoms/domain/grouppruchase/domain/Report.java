package com.chaewookim.accountbookformoms.domain.grouppruchase.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report {

    private Long id;
    private Long reporterId;       // 신고자 ID
    private Long groupPurchaseId;  // 신고 대상 공동구매 ID
    private String reason;         // 신고 사유 내용
    private boolean isProcessed;   // 관리자 처리 여부
    private LocalDateTime createdAt;
    private LocalDateTime processedAt; // 관리자 처리 일시

    @Builder
    public Report(Long id, Long reporterId, Long groupPurchaseId, String reason) {
        this.id = id;
        this.reporterId = reporterId;
        this.groupPurchaseId = groupPurchaseId;
        this.reason = reason;
        this.isProcessed = false;
        this.createdAt = LocalDateTime.now();
    }
}