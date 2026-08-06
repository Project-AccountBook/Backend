package com.chaewookim.accountbookformoms.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record TokenResponse(

        String accessToken,
        @JsonIgnore
        String refreshToken
) {
}
