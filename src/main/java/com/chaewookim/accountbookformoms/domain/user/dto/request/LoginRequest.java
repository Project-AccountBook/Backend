package com.chaewookim.accountbookformoms.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest (

    @NotBlank
    String username,

    @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    String password
) {
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
