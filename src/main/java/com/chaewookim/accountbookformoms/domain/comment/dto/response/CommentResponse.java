package com.chaewookim.accountbookformoms.domain.comment.dto.response;

import com.chaewookim.accountbookformoms.domain.comment.entity.Comment;
import com.chaewookim.accountbookformoms.domain.comment.enums.ReferenceType;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        Long userId,
        String authorNickname,
        Long referenceId,
        ReferenceType referenceType,
        Long parentId,
        String content,
        boolean deleted,
        boolean accepted,
        long likeCount,
        boolean liked,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    private static final String DELETED_MESSAGE = "삭제된 댓글입니다.";
    private static final String ADMIN_DELETED_MESSAGE = "관리자가 삭제한 댓글입니다.";
    private static final String UNKNOWN_AUTHOR = "탈퇴한 사용자";

    public static CommentResponse from(Comment comment) {
        return from(comment, null, 0L, false);
    }

    public static CommentResponse from(Comment comment, String authorNickname) {
        return from(comment, authorNickname, 0L, false);
    }

    public static CommentResponse from(Comment comment, String authorNickname, long likeCount, boolean liked) {
        boolean deleted = comment.isDeleted();
        boolean adminDeleted = comment.isAdminDeleted();
        String content;
        if (adminDeleted) {
            content = ADMIN_DELETED_MESSAGE;
        } else if (deleted) {
            content = DELETED_MESSAGE;
        } else {
            content = comment.getContent();
        }
        return new CommentResponse(
                comment.getId(),
                comment.getUserId(),
                authorNickname != null ? authorNickname : UNKNOWN_AUTHOR,
                comment.getReferenceId(),
                comment.getReferenceType(),
                comment.getParentId(),
                content,
                deleted || adminDeleted,
                comment.isAccepted(),
                likeCount,
                liked,
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
