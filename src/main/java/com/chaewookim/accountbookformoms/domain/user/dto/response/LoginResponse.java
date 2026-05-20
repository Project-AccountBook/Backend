package com.chaewookim.accountbookformoms.domain.user.dto.response;

public record LoginResponse (

    String accessToken,
    String refreshToken,
    String username
) {
}
