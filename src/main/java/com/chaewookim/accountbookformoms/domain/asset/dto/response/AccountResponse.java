package com.chaewookim.accountbookformoms.domain.asset.dto.response;

import com.chaewookim.accountbookformoms.domain.asset.entity.Account;
import com.chaewookim.accountbookformoms.domain.asset.enums.AccountKind;
import com.chaewookim.accountbookformoms.domain.asset.enums.AccountRole;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountResponse(

        Long id,
        String accountName,
        BigDecimal initialBalance,
        BigDecimal currentBalance,
        AccountRole role,
        AccountKind kind,
        BigDecimal creditLimit,
        BigDecimal loanLimit,
        BigDecimal disbursedAmount,
        BigDecimal goalAmount,
        LocalDate goalDate,
        Integer progressPercent
) {
    public AccountResponse(Account account) {
        this(
                account.getId(),
                account.getAccountName(),
                account.getInitialBalance(),
                account.getCurrentBalance(),
                account.getRole(),
                account.getKind(),
                account.getCreditLimit(),
                account.getLoanLimit(),
                account.getDisbursedAmount(),
                account.getGoalAmount(),
                account.getGoalDate(),
                account.calculateGoalProgressPercent()
        );
    }
}
