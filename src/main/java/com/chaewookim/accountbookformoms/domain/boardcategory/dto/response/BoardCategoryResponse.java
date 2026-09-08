package com.chaewookim.accountbookformoms.domain.boardcategory.dto.response;

import com.chaewookim.accountbookformoms.domain.board.enums.BOARD_TYPE;
import com.chaewookim.accountbookformoms.domain.boardcategory.entity.BoardCategory;

public record BoardCategoryResponse(
        Long id,
        String name,
        BOARD_TYPE boardType,
        int displayOrder
) {
    public static BoardCategoryResponse from(BoardCategory entity) {
        return new BoardCategoryResponse(
                entity.getId(),
                entity.getName(),
                entity.getBoardType(),
                entity.getDisplayOrder()
        );
    }
}
