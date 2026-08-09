package com.chaewookim.accountbookformoms.domain.asset.dto.request;

import com.chaewookim.accountbookformoms.domain.asset.enums.AccountRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record AccountUpdateRequest(

        @NotBlank(message = "계좌 이름은 필수 입력 값입니다.")
        String accountName,

        @NotNull(message = "초기 잔고는 필수 입력 값입니다.")
        @PositiveOrZero(message = "초기 잔고는 0원 이상이어야 합니다.")
        BigDecimal initialBalance,

        @NotNull(message = "현재 잔고는 필수 입력 값입니다.")
        @PositiveOrZero(message = "현재 잔고는 0원 이상이어야 합니다.")
        BigDecimal currentBalance,

        AccountRole role
) {
}
