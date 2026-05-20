package com.chaewookim.accountbookformoms.domain.user.dto.request;

public record UpdateRequest(
        String username,
        String email,
        String address
) {
}
