package com.chaewookim.accountbookformoms.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

public record TokenResponse(

        String accessToken,
        @JsonIgnore
        String refreshToken,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Boolean isNewUser
) {
    public TokenResponse(String accessToken, String refreshToken) {
        this(accessToken, refreshToken, null);
    }
}
