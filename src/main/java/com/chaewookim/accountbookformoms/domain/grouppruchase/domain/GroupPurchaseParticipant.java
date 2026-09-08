package com.chaewookim.accountbookformoms.domain.grouppruchase.domain;

import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.ParticipantStatus;
import com.chaewookim.accountbookformoms.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@Table(name = "group_purchase_participant")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE group_purchase_participant SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class GroupPurchaseParticipant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_purchase_id", nullable = false)
    private Long groupPurchaseId;                // 공동구매 ID

    @Column(name = "user_id", nullable = false)
    private Long userId;                         // 참여자 ID

    @Column(name = "participant_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ParticipantStatus participantStatus; // 참여 상태

    @Column(name = "account_id", nullable = false)
    private Long accountId;                      // 결제 계좌 ID

    @Builder
    public GroupPurchaseParticipant(Long id, Long groupPurchaseId, Long userId, Long accountId, ParticipantStatus participantStatus) {
        this.id = id;
        this.groupPurchaseId = groupPurchaseId;
        this.userId = userId;
        this.accountId = accountId;
        this.participantStatus = participantStatus != null ? participantStatus : ParticipantStatus.JOINED;
    }

    public void updateStatus(ParticipantStatus status) {
        this.participantStatus = status;
    }
}