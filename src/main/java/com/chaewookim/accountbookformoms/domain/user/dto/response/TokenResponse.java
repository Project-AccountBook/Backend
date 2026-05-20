package com.chaewookim.accountbookformoms.domain.user.dto.response;

public record TokenResponse(

        String accessToken,
        String refreshToken
) {
}
