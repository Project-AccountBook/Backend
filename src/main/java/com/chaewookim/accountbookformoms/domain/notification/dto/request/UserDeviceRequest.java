package com.chaewookim.accountbookformoms.domain.notification.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UserDeviceRequest(

        @NotBlank
        String fcmToken
) {
}
