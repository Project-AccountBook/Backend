package com.chaewookim.accountbookformoms.domain.notification.entity;

import com.chaewookim.accountbookformoms.domain.notification.enums.NotificationType;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@Table(name = "notification",
        indexes = @Index(name = "idx_notification_user", columnList = "user_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE notification SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    private Long referenceId;   // 데이터의 pk

    private String redirectUrl; // 이동 경로

    @Column(nullable = false)
    private Boolean isRead = false;

    @Builder
    public Notification(User user, NotificationType type, String title, String message, String redirectUrl, Long referenceId) {
        this.user = user;
        this.type = type;
        this.title = title;
        this.message = message;
        this.redirectUrl = redirectUrl;
        this.referenceId = referenceId;
        this.isRead = false;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
