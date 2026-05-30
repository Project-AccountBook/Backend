package com.chaewookim.accountbookformoms.domain.user.enums;

import lombok.Getter;

@Getter
public enum UserRole {

    ROLE_USER("일반 사용자"),
    ROLE_ADMIN("관리자");

    private final String title;

    UserRole(String title) {
        this.title = title;
    }
}
