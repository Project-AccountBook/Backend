package com.chaewookim.accountbookformoms.domain.board.dto.request;

import com.chaewookim.accountbookformoms.domain.board.enums.BOARD_TYPE;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BoardCreateRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 255) String title,
        @NotBlank String content,
        @NotNull BOARD_TYPE type
) {
}
