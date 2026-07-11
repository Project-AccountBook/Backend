package com.chaewookim.accountbookformoms.domain.asset.dto.response;

import com.chaewookim.accountbookformoms.domain.asset.entity.Account;
import com.chaewookim.accountbookformoms.domain.asset.enums.AccountRole;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public record AccountResponse(

        Long id,
        String accountName,
        BigDecimal initialBalance,
        BigDecimal currentBalance,
        AccountRole role,
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
                account.getGoalAmount(),
                account.getGoalDate(),
                calculateProgressPercent(account.getCurrentBalance(), account.getGoalAmount())
        );
    }

    private static Integer calculateProgressPercent(BigDecimal currentBalance, BigDecimal goalAmount) {
        if (goalAmount == null || goalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal balance = currentBalance != null ? currentBalance : BigDecimal.ZERO;
        int percent = balance
                .multiply(BigDecimal.valueOf(100))
                .divide(goalAmount, 0, RoundingMode.HALF_UP)
                .intValue();
        return Math.min(100, Math.max(0, percent));
    }
}
