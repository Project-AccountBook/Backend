package com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response;

import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.Report;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.ReportProcessResult;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.ReportTargetType;

import java.time.LocalDateTime;

public record ReportResponse(
        Long id,
        Long reporterId,
        ReportTargetType targetType,
        Long targetId,
        String reason,
        boolean isProcessed,
        LocalDateTime processedAt,
        ReportProcessResult processResult,
        String processReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getReporterId(),
                report.getTargetType(),
                report.getTargetId(),
                report.getReason(),
                report.isProcessed(),
                report.getProcessedAt(),
                report.getProcessResult(),
                report.getProcessReason(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}
