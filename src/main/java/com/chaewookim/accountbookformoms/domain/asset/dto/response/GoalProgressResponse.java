package com.chaewookim.accountbookformoms.domain.asset.dto.response;

import com.chaewookim.accountbookformoms.domain.asset.enums.AccountRole;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalProgressResponse(

        Long accountId,
        String accountName,
        AccountRole role,
        BigDecimal currentBalance,
        BigDecimal goalAmount,
        Integer progressPercent,
        LocalDate goalDate,
        Integer dDay
) implements Serializable {
}
