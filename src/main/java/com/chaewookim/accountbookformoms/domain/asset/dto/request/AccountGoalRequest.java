package com.chaewookim.accountbookformoms.domain.asset.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountGoalRequest(

        @NotNull(message = "목표 금액은 필수입니다.")
        @DecimalMin(value = "0", message = "목표 금액은 0원 이상이어야 합니다.")
        BigDecimal goalAmount,

        LocalDate goalDate
) {
}
