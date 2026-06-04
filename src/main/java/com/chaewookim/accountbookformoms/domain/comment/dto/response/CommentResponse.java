package com.chaewookim.accountbookformoms.domain.comment.dto.response;

import com.chaewookim.accountbookformoms.domain.comment.entity.Comment;
import com.chaewookim.accountbookformoms.domain.comment.enums.ReferenceType;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        Long userId,
        Long referenceId,
        ReferenceType referenceType,
        Long parentId,
        String content,
        boolean deleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    private static final String DELETED_MESSAGE = "삭제된 댓글입니다.";

    public static CommentResponse from(Comment comment) {
        boolean deleted = comment.isDeleted();
        return new CommentResponse(
                comment.getId(),
                comment.getUserId(),
                comment.getReferenceId(),
                comment.getReferenceType(),
                comment.getParentId(),
                deleted ? DELETED_MESSAGE : comment.getContent(),
                deleted,
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
