package com.chaewookim.accountbookformoms.domain.board.dto.response;

import com.chaewookim.accountbookformoms.domain.board.entity.Board;
import com.chaewookim.accountbookformoms.domain.board.enums.BOARD_TYPE;

import java.time.LocalDateTime;

public record BoardResponse(
        Long id,
        Long userId,
        Long categoryId,
        String title,
        String content,
        BOARD_TYPE type,
        int views,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static BoardResponse from(Board board) {
        return new BoardResponse(
                board.getId(),
                board.getUserId(),
                board.getCategoryId(),
                board.getTitle(),
                board.getContent(),
                board.getType(),
                board.getViews(),
                board.getCreatedAt(),
                board.getUpdatedAt()
        );
    }
}
