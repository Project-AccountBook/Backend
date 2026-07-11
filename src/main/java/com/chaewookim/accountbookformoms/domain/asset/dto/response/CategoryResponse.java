package com.chaewookim.accountbookformoms.domain.asset.dto.response;

import com.chaewookim.accountbookformoms.domain.asset.entity.TransactionCategory;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;

public record CategoryResponse(

        Long id,
        String name,
        TransactionType type,
        boolean isCustom,
        boolean includeInSavingsRate,
        boolean includeInInvestmentRate
) {
    public static CategoryResponse from(TransactionCategory category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType(),
                category.getUser() != null,
                category.isIncludeInSavingsRate(),
                category.isIncludeInInvestmentRate()
        );
    }
}
