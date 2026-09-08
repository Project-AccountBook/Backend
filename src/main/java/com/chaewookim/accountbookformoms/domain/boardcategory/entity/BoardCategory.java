package com.chaewookim.accountbookformoms.domain.boardcategory.entity;

import com.chaewookim.accountbookformoms.domain.board.enums.BOARD_TYPE;
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

@Entity
@Getter
@Table(
        name = "board_category",
        indexes = @Index(name = "idx_board_category_board_type", columnList = "board_type")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "board_type", nullable = false, length = 20)
    private BOARD_TYPE boardType;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Builder
    public BoardCategory(String name, BOARD_TYPE boardType, int displayOrder) {
        this.name = name;
        this.boardType = boardType;
        this.displayOrder = displayOrder;
    }
}
