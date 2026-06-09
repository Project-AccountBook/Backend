package com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record GroupPurchaseApplicationCreateRequest(
        @NotBlank(message = "공구 제목/상품명은 필수 입력값입니다.")
        String title,

        @NotBlank(message = "신청 사유 및 내용은 필수 입력값입니다.")
        String content,

        String productUrl,

        @Min(value = 0, message = "희망 금액은 0원 이상이어야 합니다.")
        int expectedPrice
) {
}
