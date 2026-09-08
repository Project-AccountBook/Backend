package com.chaewookim.accountbookformoms.domain.board.dto.response;

import com.chaewookim.accountbookformoms.domain.board.document.BoardDocument;

import java.time.LocalDateTime;
import java.util.List;

public record BoardSearchResponse(
        Long id,
        Long userId,
        Long categoryId,
        String title,
        String content,
        String type,
        List<String> tags,
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
                doc.getTags() != null ? doc.getTags() : List.of(),
                doc.getCreatedAt()
        );
    }
}
