package com.chaewookim.accountbookformoms.domain.user.event;

import jakarta.validation.constraints.NotNull;

public record UserSignedUpEvent(

        @NotNull
        Long userId
) {
}
