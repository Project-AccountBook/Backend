package com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record GroupPurchaseCreateRequest(
        @NotNull(message = "카테고리 ID는 필수 입력값입니다.")
        Long categoryId,

        @NotBlank(message = "제목은 필수 입력값입니다.")
        String title,

        @NotBlank(message = "내용은 필수 입력값입니다.")
        String content,

        @Min(value = 0, message = "금액은 0원 이상이어야 합니다.")
        int price,

        @Min(value = 1, message = "최소 성사 인원은 1명 이상이어야 합니다.")
        int minParticipants,

        @Min(value = 1, message = "최대 제한 인원은 1명 이상이어야 합니다.")
        int maxParticipants,

        @NotNull(message = "마감 기한은 필수 입력값입니다.")
        @Future(message = "마감 기한은 미래 시점이어야 합니다.")
        LocalDateTime deadline,

        @NotBlank(message = "수령 장소는 필수 입력값입니다.")
        String pickupLocation,

        String imageUrl,

        @NotNull(message = "결제 계좌 ID는 필수입니다.")
        Long accountId
) {
}
