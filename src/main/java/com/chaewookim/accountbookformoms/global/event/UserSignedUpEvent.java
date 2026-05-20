package com.chaewookim.accountbookformoms.global.event;

import jakarta.validation.constraints.NotNull;

public record UserSignedUpEvent(
        @NotNull
        Long userId
) {}
