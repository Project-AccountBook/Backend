package com.chaewookim.accountbookformoms.domain.asset.dto.request;

import com.chaewookim.accountbookformoms.domain.asset.enums.AccountKind;
import com.chaewookim.accountbookformoms.domain.asset.enums.AccountRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AccountUpdateRequest(

        @NotBlank(message = "계좌 이름은 필수 입력 값입니다.")
        String accountName,

        @NotNull(message = "초기 잔고는 필수 입력 값입니다.")
        BigDecimal initialBalance,

        @NotNull(message = "현재 잔고는 필수 입력 값입니다.")
        BigDecimal currentBalance,

        AccountRole role,

        AccountKind kind,

        BigDecimal creditLimit,

        BigDecimal loanLimit
) {
    public AccountUpdateRequest(String accountName, BigDecimal initialBalance, BigDecimal currentBalance,
                                AccountRole role, AccountKind kind, BigDecimal creditLimit) {
        this(accountName, initialBalance, currentBalance, role, kind, creditLimit, null);
    }
}
