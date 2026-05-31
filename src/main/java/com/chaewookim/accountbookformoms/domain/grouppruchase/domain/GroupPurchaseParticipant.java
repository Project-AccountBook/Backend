package com.chaewookim.accountbookformoms.domain.grouppruchase.domain;

import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.ParticipantStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupPurchaseParticipant {

    private Long id;
    private Long groupPurchaseId;                // 공동구매 ID
    private Long userId;                         // 참여자 ID
    private ParticipantStatus participantStatus; // 참여 상태
    private LocalDateTime joinedAt;
    private LocalDateTime updatedAt;

    @Builder
    public GroupPurchaseParticipant(Long id, Long groupPurchaseId, Long userId) {
        this.id = id;
        this.groupPurchaseId = groupPurchaseId;
        this.userId = userId;
        this.participantStatus = ParticipantStatus.JOINED;
        this.joinedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}