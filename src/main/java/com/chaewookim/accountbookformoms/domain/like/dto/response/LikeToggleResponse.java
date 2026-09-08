package com.chaewookim.accountbookformoms.domain.like.dto.response;

public record LikeToggleResponse(
        boolean liked,
        long likeCount
) {
}
