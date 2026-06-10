package com.chaewookim.accountbookformoms.domain.grouppruchase.domain;

import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.ReportProcessResult;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.ReportTargetType;
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
@Table(name = "report")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE report SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Report extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long reporterId;       // 신고자 ID

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReportTargetType targetType;  // 신고 대상 유형 (공동구매, 댓글)

    @Column(nullable = false)
    private Long targetId;         // 신고 대상 ID

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;         // 신고 사유 내용

    @Column(nullable = false)
    private boolean isProcessed;   // 관리자 처리 여부

    private LocalDateTime processedAt; // 관리자 처리 일시

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReportProcessResult processResult; // 처리 결과

    @Column(columnDefinition = "TEXT")
    private String processReason;  // 처리 사유

    @Builder
    public Report(Long reporterId, ReportTargetType targetType, Long targetId, String reason) {
        this.reporterId = reporterId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.isProcessed = false;
        this.processResult = ReportProcessResult.PENDING;
    }

    public void process(ReportProcessResult result, String processReason) {
        this.isProcessed = true;
        this.processResult = result;
        this.processReason = processReason;
        this.processedAt = LocalDateTime.now();
    }
}