package com.chaewookim.accountbookformoms.domain.board.dto.response;

import com.chaewookim.accountbookformoms.domain.board.document.BoardDocument;

import java.time.LocalDateTime;

public record BoardSearchResponse(
        Long id,
        Long userId,
        Long categoryId,
        String title,
        String content,
        String type,
        LocalDateTime createdAt
) {
    public static BoardSearchResponse from(BoardDocument doc) {
        return new BoardSearchResponse(
                doc.getId(),
                doc.getUserId(),
                doc.getCategoryId(),
                doc.getTitle(),
                doc.getContent(),
                doc.getType(),
                doc.getCreatedAt()
        );
    }
}
