package com.chaewookim.accountbookformoms.domain.user.dto.response;

import com.chaewookim.accountbookformoms.domain.user.entity.InterestCategory;
import lombok.Builder;

@Builder
public record InterestCategoryResponse(
        Long id,
        Long categoryId,
        String categoryName,
        boolean isAlarmEnabled
) {
    public static InterestCategoryResponse from(InterestCategory entity) {
        return InterestCategoryResponse.builder()
                .id(entity.getId())
                .categoryId(entity.getCategory().getId())
                .categoryName(entity.getCategory().getName())
                .isAlarmEnabled(entity.isAlarmEnabled())
                .build();
    }
}
