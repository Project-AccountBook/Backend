package com.chaewookim.accountbookformoms.domain.asset.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;

public record AllocationBucketResponse(

        BigDecimal net,
        BigDecimal rate,
        BigDecimal inflow,
        BigDecimal outflow
) implements Serializable {
}
