package com.chaewookim.accountbookformoms.domain.notification.dto.response;

import com.chaewookim.accountbookformoms.domain.notification.entity.Notification;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String title,
        String message,
        String redirectUrl,
        Long referenceId,
        String type,
        Boolean isRead,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getRedirectUrl(),
                notification.getReferenceId(),
                notification.getType().name(),
                notification.getIsRead(),
                notification.getCreatedAt()
        );
    }
}
