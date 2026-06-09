package com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductCreateRequest(
        @NotBlank(message = "상품명은 필수 입력값입니다.")
        String name,

        @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
        int price,

        String description,

        String imageUrl,

        @NotNull(message = "카테고리는 필수 입력값입니다.")
        Long categoryId,

        Long applicationId,

        Long reportId
) {
}
