package com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response;

public record GroupPurchaseDashboardResponse(
        long todayCreatedCount,
        long activeParticipantsCount,
        long recruitingCount,
        long successCount,
        long failedCount,
        double recruitingRatio,
        double successRatio,
        double failedRatio
) {
}
