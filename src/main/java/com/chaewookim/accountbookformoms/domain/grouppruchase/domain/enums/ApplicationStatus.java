package com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums;

public enum ApplicationStatus {
    PENDING("대기중"),
    APPROVED("승인됨"),
    REJECTED("반려됨");

    private final String description;

    ApplicationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
