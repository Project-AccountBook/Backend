package com.chaewookim.accountbookformoms.domain.user.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UpdateProfileRequest(

        @NotBlank(message = "닉네임은 필수 입력 값입니다.")
        String username,

        LocalDate birthDate,

        String address,

        @Min(0)
        @Max(100)
        @NotNull(message = "예산 대비 지출 알림 기준은 필수 입력 값입니다.")
        Integer budgetAlertThreshold,

        @NotNull(message = "프로필 공개 여부는 필수 입력 값입니다.")
        Boolean isPortfolioPublic,

        @NotNull(message = "예산 알림 여부는 필수 입력 값입니다.")
        Boolean isBudgetAlertEnabled,

        @NotNull(message = "관심 카테고리 알림 여부는 필수 입력 값입니다.")
        Boolean isInterestCategoryEnabled,

        @NotNull(message = "보안 알림 여부는 필수 입력 값입니다.")
        Boolean isSystemAlertEnabled
) {
}
