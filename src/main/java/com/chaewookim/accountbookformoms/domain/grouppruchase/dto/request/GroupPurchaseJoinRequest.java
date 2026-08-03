package com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request;

import jakarta.validation.constraints.NotNull;

public record GroupPurchaseJoinRequest(
        @NotNull(message = "결제 계좌 ID는 필수입니다.")
        Long accountId
) {
}
