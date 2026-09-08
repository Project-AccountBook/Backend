package com.chaewookim.accountbookformoms.domain.user.dto.response;

public record LocationResponse(
        Long userId,
        Double latitude,
        Double longitude
) {
}
