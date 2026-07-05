package com.chaewookim.accountbookformoms.domain.tag.entity;

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
        name = "board_tag",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_board_tag_board_tag",
                columnNames = {"board_id", "tag_id"}
        ),
        indexes = {
                @Index(name = "idx_board_tag_board", columnList = "board_id"),
                @Index(name = "idx_board_tag_tag", columnList = "tag_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardTag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Column(name = "tag_id", nullable = false)
    private Long tagId;

    @Builder
    public BoardTag(Long boardId, Long tagId) {
        this.boardId = boardId;
        this.tagId = tagId;
    }
}
