package com.chaewookim.accountbookformoms.domain.dashboard.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;

public record SummaryResponse(

        BigDecimal totalExpense
) implements Serializable {
}
