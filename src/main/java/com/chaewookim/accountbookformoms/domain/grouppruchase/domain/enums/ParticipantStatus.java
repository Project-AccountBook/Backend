package com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums;

public enum ParticipantStatus {
    JOINED("참여 완료"),
    CANCELED("참여 취소"),
    RECEIVED("물품 수령 완료");

    private final String description;

    ParticipantStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}