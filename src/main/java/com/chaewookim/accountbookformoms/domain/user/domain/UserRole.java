package com.chaewookim.accountbookformoms.domain.user.domain;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum UserRole {

    USER("ROLE_USER", "일반 사용자"),
    ADMIN("ROLE_ADMIN", "관리자");

    private final String key;
    private final String title;

    UserRole(String key, String title) {
        this.key = key;
        this.title = title;
    }

    public static UserRole findByKey(String key) {
        return Arrays.stream(UserRole.values())
                .filter(role -> role.getKey().equals(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("해당 권한을 찾을 수 없습니다: " + key));
    }
}
