package com.chaewookim.accountbookformoms.domain.asset.dto.request;

import jakarta.validation.constraints.NotNull;

public record CategoryAllocationRequest(

        @NotNull(message = "저축률 포함 여부는 필수입니다.")
        Boolean includeInSavingsRate,

        @NotNull(message = "투자율 포함 여부는 필수입니다.")
        Boolean includeInInvestmentRate
) {
}
