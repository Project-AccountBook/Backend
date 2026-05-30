package com.chaewookim.accountbookformoms.domain.user.dto.response;

public record SignupResponse(

        Long userId,
        String email,
        String username
) {
}
