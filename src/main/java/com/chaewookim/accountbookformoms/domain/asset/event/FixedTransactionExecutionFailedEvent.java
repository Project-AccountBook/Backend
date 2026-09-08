package com.chaewookim.accountbookformoms.domain.asset.event;

import com.chaewookim.accountbookformoms.domain.asset.enums.FixedTransactionExecutionFailure;

public record FixedTransactionExecutionFailedEvent(
        Long userId,
        Long fixedTransactionId,
        String accountName,
        String description,
        FixedTransactionExecutionFailure failureReason
) {
}
