package com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request;

import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.ReportProcessResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReportProcessRequest(
        @NotNull(message = "처리 조치 결과는 필수 입력값입니다.")
        ReportProcessResult action,

        @NotBlank(message = "처리 사유는 필수 입력값입니다.")
        String reason
) {
}
