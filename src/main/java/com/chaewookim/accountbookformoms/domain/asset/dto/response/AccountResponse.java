package com.chaewookim.accountbookformoms.domain.asset.dto.response;

import com.chaewookim.accountbookformoms.domain.asset.entity.Account;

import java.math.BigDecimal;

public record AccountResponse(

        Long id,
        String accountName,
        BigDecimal initialBalance,
        BigDecimal currentBalance
) {
    public AccountResponse(Account account) {
        this(
                account.getId(),
                account.getAccountName(),
                account.getInitialBalance(),
                account.getCurrentBalance()
        );
    }
}
