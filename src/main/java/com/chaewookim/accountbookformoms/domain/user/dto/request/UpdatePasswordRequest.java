package com.chaewookim.accountbookformoms.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdatePasswordRequest(

        String currentPassword,

        @NotBlank(message = "새로운 비밀번호는 필수 입력값입니다.")
        @Pattern(regexp = "(?=.*[0-9])(?=.*[a-zA-Z])(?=.*\\W)(?=\\S+$).{8,16}",
                message = "비밀번호는 8~16자 영문, 숫자, 특수문자 조합이어야 합니다.")
        String newPassword
) {
}
