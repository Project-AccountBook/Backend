package com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request;

import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReportCreateRequest(
        @NotNull(message = "신고 대상 유형은 필수 입력값입니다.")
        ReportTargetType targetType,

        @NotNull(message = "신고 대상 ID는 필수 입력값입니다.")
        Long targetId,

        @NotBlank(message = "신고 사유는 필수 입력값입니다.")
        String reason
) {
}
