package com.chaewookim.accountbookformoms.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TokenReissueRequest (

        @NotBlank(message = "토큰은 비어있을 수 없습니다.")
        String refreshToken
) {}
