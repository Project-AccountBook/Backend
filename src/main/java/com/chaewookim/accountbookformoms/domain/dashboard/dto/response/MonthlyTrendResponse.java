package com.chaewookim.accountbookformoms.domain.dashboard.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;

public record MonthlyTrendResponse(

        String yearMonth,
        BigDecimal income,
        BigDecimal expense
) implements Serializable {
}
