package com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GroupPurchaseCategoryCreateRequest(
        @NotBlank(message = "카테고리명은 필수 입력값입니다.")
        String name,

        @NotNull(message = "정렬 순서는 필수 입력값입니다.")
        Integer sortOrder
) {
}
