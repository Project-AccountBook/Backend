package com.chaewookim.accountbookformoms.domain.board.dto.request;

import jakarta.validation.constraints.NotNull;

public record BoardStatusRequest(
        @NotNull Boolean value
) {
}
