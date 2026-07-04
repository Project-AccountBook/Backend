package com.chaewookim.accountbookformoms.domain.comment.application;

import com.chaewookim.accountbookformoms.domain.comment.dao.CommentRepository;
import com.chaewookim.accountbookformoms.domain.comment.dto.response.AdminCommentResponse;
import com.chaewookim.accountbookformoms.domain.comment.entity.Comment;
import com.chaewookim.accountbookformoms.domain.comment.enums.ReferenceType;
import com.chaewookim.accountbookformoms.domain.comment.error.CommentErrorCode;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminCommentService {

    private final CommentRepository commentRepository;

    public Long deleteByAdmin(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(CommentErrorCode.COMMENT_NOT_FOUND));
        comment.markAsAdminDeleted();
        return comment.getId();
    }

    /**
     * 관리자 댓글 목록 조회.
     * <p>Comment 엔티티에는 {@code @SQLRestriction} 이 없어 소프트 삭제된 댓글도 findAll 로 함께 조회된다.
     * 원문(content)을 마스킹 없이 반환.</p>
     *
     * @param referenceType null 이면 전체
     * @param referenceId   null 이면 전체 (referenceType 과 함께 있어야 함)
     */
    @Transactional(readOnly = true)
    public Page<AdminCommentResponse> list(ReferenceType referenceType,
                                            Long referenceId,
                                            Pageable pageable) {
        Page<Comment> page;
        if (referenceType != null && referenceId != null) {
            page = commentRepository
                    .findByReferenceIdAndReferenceType(referenceId, referenceType, pageable);
        } else if (referenceType != null) {
            page = commentRepository.findByReferenceType(referenceType, pageable);
        } else {
            page = commentRepository.findAll(pageable);
        }
        return page.map(AdminCommentResponse::from);
    }
}
