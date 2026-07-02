package com.chaewookim.accountbookformoms.domain.comment.dto.response;

import com.chaewookim.accountbookformoms.domain.comment.entity.Comment;
import com.chaewookim.accountbookformoms.domain.comment.enums.ReferenceType;

import java.time.LocalDateTime;

/**
 * 관리자 조회용 댓글 응답. 원문 내용을 마스킹 없이 반환하고 관리자/유저 삭제 여부를 그대로 노출.
 */
public record AdminCommentResponse(
        Long id,
        Long userId,
        Long referenceId,
        ReferenceType referenceType,
        Long parentId,
        String content,
        boolean adminDeleted,
        boolean userDeleted,
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {
    public static AdminCommentResponse from(Comment comment) {
        return new AdminCommentResponse(
                comment.getId(),
                comment.getUserId(),
                comment.getReferenceId(),
                comment.getReferenceType(),
                comment.getParentId(),
                comment.getContent(),
                comment.isAdminDeleted(),
                comment.isDeleted(),
                comment.getCreatedAt(),
                comment.getDeletedAt()
        );
    }
}
