package com.chaewookim.accountbookformoms.domain.asset.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;

public record MonthlyAllocationSummaryResponse(

        String yearMonth,
        BigDecimal totalIncome,
        AllocationBucketResponse savings,
        AllocationBucketResponse investment
) implements Serializable {

    public static MonthlyAllocationSummaryResponse from(
            String yearMonth,
            BigDecimal totalIncome,
            MonthlyAllocationResponse allocation
    ) {
        return new MonthlyAllocationSummaryResponse(
                yearMonth,
                totalIncome,
                allocation.savings(),
                allocation.investment()
        );
    }
}
