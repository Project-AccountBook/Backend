package com.chaewookim.accountbookformoms.domain.board.dto.response;

import com.chaewookim.accountbookformoms.domain.board.entity.Board;
import com.chaewookim.accountbookformoms.domain.board.enums.BOARD_TYPE;

import java.time.LocalDateTime;

public record BoardHotResponse(
        Long id,
        String title,
        BOARD_TYPE type,
        int views,
        long likeCount,
        long score,
        LocalDateTime createdAt
) {
    public static BoardHotResponse from(Board b, long likeCount) {
        return new BoardHotResponse(
                b.getId(), b.getTitle(), b.getType(), b.getViews(),
                likeCount, (long) b.getViews() + likeCount * 3L, b.getCreatedAt()
        );
    }
}
