package com.chaewookim.accountbookformoms.domain.notification.event;

import com.chaewookim.accountbookformoms.domain.notification.enums.NotificationType;
import com.chaewookim.accountbookformoms.domain.user.entity.User;

public record NotificationEvent(

        User user,
        NotificationType type,
        String title,
        String message,
        String redirectUrl,
        Long referenceId
) {
}
