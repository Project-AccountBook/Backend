package com.chaewookim.accountbookformoms.domain.user.dto.response;

public record NearbyUserResponse(
        Long userId,
        Double latitude,
        Double longitude,
        Double distanceKm
) {
}
