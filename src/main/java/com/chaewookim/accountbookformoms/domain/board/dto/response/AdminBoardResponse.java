package com.chaewookim.accountbookformoms.domain.board.dto.response;

import com.chaewookim.accountbookformoms.domain.board.entity.Board;
import com.chaewookim.accountbookformoms.domain.board.enums.BOARD_TYPE;

import java.time.LocalDateTime;

/**
 * 관리자 조회용 게시물 응답. 원문(제목/내용)을 마스킹 없이 그대로 반환하고
 * 관리자 삭제/유저 소프트 삭제 여부를 별도 플래그로 노출.
 */
public record AdminBoardResponse(
        Long id,
        Long userId,
        Long categoryId,
        String title,
        String content,
        BOARD_TYPE type,
        int views,
        boolean adminDeleted,
        boolean userDeleted,
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {
    public static AdminBoardResponse from(Board board) {
        return new AdminBoardResponse(
                board.getId(),
                board.getUserId(),
                board.getCategoryId(),
                board.getTitle(),
                board.getContent(),
                board.getType(),
                board.getViews(),
                board.isAdminDeleted(),
                board.getDeletedAt() != null,
                board.getCreatedAt(),
                board.getDeletedAt()
        );
    }
}
