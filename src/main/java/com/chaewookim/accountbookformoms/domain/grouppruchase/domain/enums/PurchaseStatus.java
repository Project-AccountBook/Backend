package com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums;

public enum PurchaseStatus {
    RECRUITING("모집중"),
    SUCCESS("모집 성공"),
    FAILED("무산됨"),
    CLOSED("거래 종료"),
    BLIND("블라인드 처리");

    private final String description;

    PurchaseStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
