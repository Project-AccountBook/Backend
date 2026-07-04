package com.chaewookim.accountbookformoms.domain.image.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AttachImageRequest(
        @NotBlank @Size(max = 500) String imageUrl,
        @Size(max = 500) String s3Key
) {
}
