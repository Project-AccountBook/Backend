package com.chaewookim.accountbookformoms.domain.asset.dto.request;

import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountGoalRequest(

        @Positive(message = "목표 금액은 0보다 커야 합니다.")
        BigDecimal goalAmount,

        LocalDate goalDate
) {
}
