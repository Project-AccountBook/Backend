package com.chaewookim.accountbookformoms.domain.userstats.dto.response;

public record UserStatsResponse(
        Long userId,
        String nickname,
        long postCount,
        long followerCount,
        long followingCount,
        boolean following
) {
}
