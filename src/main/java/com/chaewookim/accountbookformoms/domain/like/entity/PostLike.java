package com.chaewookim.accountbookformoms.domain.like.entity;

import com.chaewookim.accountbookformoms.domain.like.enums.LikeTargetType;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "post_like",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_like_user_target",
                columnNames = {"user_id", "target_id", "target_type"}
        ),
        indexes = {
                @Index(name = "idx_like_target", columnList = "target_id, target_type"),
                @Index(name = "idx_like_user", columnList = "user_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLike extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private LikeTargetType targetType;

    @Builder
    public PostLike(Long userId, Long targetId, LikeTargetType targetType) {
        this.userId = userId;
        this.targetId = targetId;
        this.targetType = targetType;
    }
}
