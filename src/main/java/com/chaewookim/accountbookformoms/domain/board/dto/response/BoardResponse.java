package com.chaewookim.accountbookformoms.domain.board.dto.response;

import com.chaewookim.accountbookformoms.domain.board.entity.Board;
import com.chaewookim.accountbookformoms.domain.board.enums.BOARD_TYPE;

import java.time.LocalDateTime;

public record BoardResponse(
        Long id,
        Long userId,
        String authorNickname,
        Long categoryId,
        String title,
        String content,
        BOARD_TYPE type,
        int views,
        boolean resolved,
        boolean urgent,
        long likeCount,
        boolean liked,
        boolean bookmarked,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    private static final String ADMIN_DELETED_MESSAGE = "관리자가 삭제한 게시물입니다.";
    private static final String UNKNOWN_AUTHOR = "탈퇴한 사용자";

    public static BoardResponse from(Board board) {
        return from(board, 0L, null, 0L, false, false);
    }

    public static BoardResponse from(Board board, long pendingViews) {
        return from(board, pendingViews, null, 0L, false, false);
    }

    public static BoardResponse from(Board board, long pendingViews, String authorNickname) {
        return from(board, pendingViews, authorNickname, 0L, false, false);
    }

    public static BoardResponse from(
            Board board,
            long pendingViews,
            String authorNickname,
            long likeCount,
            boolean liked,
            boolean bookmarked
    ) {
        boolean adminDeleted = board.isAdminDeleted();
        return new BoardResponse(
                board.getId(),
                board.getUserId(),
                authorNickname != null ? authorNickname : UNKNOWN_AUTHOR,
                board.getCategoryId(),
                adminDeleted ? ADMIN_DELETED_MESSAGE : board.getTitle(),
                adminDeleted ? ADMIN_DELETED_MESSAGE : board.getContent(),
                board.getType(),
                board.getViews() + (int) pendingViews,
                board.isResolved(),
                board.isUrgent(),
                likeCount,
                liked,
                bookmarked,
                board.getCreatedAt(),
                board.getUpdatedAt()
        );
    }
}
