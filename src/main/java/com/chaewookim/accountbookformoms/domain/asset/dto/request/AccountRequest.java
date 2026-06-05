package com.chaewookim.accountbookformoms.domain.asset.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AccountRequest(

        @NotBlank(message = "계좌 이름은 필수 입력 값입니다.")
        String accountName,

        @NotNull(message = "초기 잔고는 필수 입력 값입니다.")
        BigDecimal initialBalance
) {
}
