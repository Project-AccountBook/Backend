package com.chaewookim.accountbookformoms.domain.user.dto.response;

import java.time.LocalDate;

public record UserProfileResponse(

        String email,
        String username,
        LocalDate birthDate,
        String address,
        Boolean hasPassword,
        Integer budgetAlertThreshold,
        Boolean isPortfolioPublic,
        Boolean isBudgetAlertEnabled,
        Boolean isInterestCategoryEnabled,
        Boolean isSystemAlertEnabled
) {
}
