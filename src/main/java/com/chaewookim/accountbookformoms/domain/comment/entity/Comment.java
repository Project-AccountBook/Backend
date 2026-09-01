package com.chaewookim.accountbookformoms.domain.comment.entity;

import com.chaewookim.accountbookformoms.domain.comment.enums.ReferenceType;
import com.chaewookim.accountbookformoms.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

@Entity
@Getter
@Table(
        name = "comment",
        indexes = {
                @Index(
                        name = "idx_comment_reference",
                        columnList = "reference_id, reference_type"
                ),
                @Index(
                        name = "idx_comment_user",
                        columnList = "user_id, created_at"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE comment SET deleted_at = NOW() WHERE id = ?")
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @Column(name = "reference_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ReferenceType referenceType;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "admin_deleted", nullable = false)
    private boolean adminDeleted;

    @Column(name = "is_accepted", nullable = false)
    private boolean accepted;

    @Column(name = "is_secret", nullable = false)
    private boolean isSecret;

    @Builder
    public Comment(Long userId, Long referenceId, ReferenceType referenceType, Long parentId, String content, Boolean isSecret) {
        this.userId = userId;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
        this.parentId = parentId;
        this.content = content;
        this.adminDeleted = false;
        this.accepted = false;
        this.isSecret = isSecret != null ? isSecret : false;
    }

    public void update(String content) {
        this.content = content;
    }

    public void markAsAdminDeleted() {
        this.adminDeleted = true;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public boolean isDeleted() {
        return getDeletedAt() != null;
    }
}
