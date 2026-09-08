package com.chaewookim.accountbookformoms.domain.comment.dto.request;

import com.chaewookim.accountbookformoms.domain.comment.enums.ReferenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CommentCreateRequest(
        @NotNull ReferenceType referenceType,
        @NotBlank String content,
        Boolean isSecret
) {
}
