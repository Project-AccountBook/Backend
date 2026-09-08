package com.chaewookim.accountbookformoms.domain.asset.dto.response;

import java.io.Serializable;

public record MonthlyAllocationResponse(

        AllocationBucketResponse savings,
        AllocationBucketResponse investment
) implements Serializable {
}
