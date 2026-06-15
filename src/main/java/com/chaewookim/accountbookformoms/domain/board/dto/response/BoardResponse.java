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
    private static final String ADMIN_DELETED_MESSAGE = "관리자가 삭제한 게시물입니다.";

    public static BoardResponse from(Board board) {
        return from(board, 0L);
    }

    public static BoardResponse from(Board board, long pendingViews) {
        boolean adminDeleted = board.isAdminDeleted();
        return new BoardResponse(
                board.getId(),
                board.getUserId(),
                board.getCategoryId(),
                adminDeleted ? ADMIN_DELETED_MESSAGE : board.getTitle(),
                adminDeleted ? ADMIN_DELETED_MESSAGE : board.getContent(),
                board.getType(),
                board.getViews() + (int) pendingViews,
                board.getCreatedAt(),
                board.getUpdatedAt()
        );
    }
}
