package com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response;

import java.math.BigDecimal;

public record GroupPurchaseJoinResponse(
        GroupPurchaseResponse groupPurchase,
        boolean budgetWarning,
        BigDecimal remainingBudget
) {
}
