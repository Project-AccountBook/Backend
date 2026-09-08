package com.chaewookim.accountbookformoms.domain.follow.dto.response;

public record FollowToggleResponse(
        boolean following,
        long followerCount
) {
}
