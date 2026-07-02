package com.chaewookim.accountbookformoms.domain.bookmark.entity;

import com.chaewookim.accountbookformoms.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "bookmark",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_bookmark_user_board",
                columnNames = {"user_id", "board_id"}
        ),
        indexes = @Index(name = "idx_bookmark_user", columnList = "user_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bookmark extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Builder
    public Bookmark(Long userId, Long boardId) {
        this.userId = userId;
        this.boardId = boardId;
    }
}
