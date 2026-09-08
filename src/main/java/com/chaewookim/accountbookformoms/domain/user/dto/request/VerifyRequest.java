package com.chaewookim.accountbookformoms.domain.user.dto.request;

import com.chaewookim.accountbookformoms.domain.user.enums.VerificationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerifyRequest(

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "인증번호는 필수입니다.")
        String code,

        VerificationType type
) {
}
