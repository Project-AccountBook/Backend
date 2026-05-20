package com.chaewookim.accountbookformoms.domain.user.dto.request;

import com.chaewookim.accountbookformoms.domain.user.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SignUpRequest(

        @NotBlank
        String username,

        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수 입력값입니다.")
        String password,

        @NotNull(message = "생년월일은 필수 입력값입니다.")
        LocalDate birthDate,

        UserRole role,

        @NotBlank(message = "주소는 필수 입력 값입니다.")
        String address
) {
}
