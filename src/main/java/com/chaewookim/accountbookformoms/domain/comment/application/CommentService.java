package com.chaewookim.accountbookformoms.domain.comment.application;

import com.chaewookim.accountbookformoms.domain.comment.dao.CommentRepository;
import com.chaewookim.accountbookformoms.domain.comment.dto.request.CommentCreateRequest;
import com.chaewookim.accountbookformoms.domain.comment.dto.request.CommentUpdateRequest;
import com.chaewookim.accountbookformoms.domain.comment.dto.response.CommentResponse;
import com.chaewookim.accountbookformoms.domain.comment.entity.Comment;
import com.chaewookim.accountbookformoms.domain.comment.enums.ReferenceType;
import com.chaewookim.accountbookformoms.domain.comment.error.CommentErrorCode;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;

    @Transactional
    public Long create(Long postId, CommentCreateRequest request, Long userId) {
        Comment comment = Comment.builder()
                .userId(userId)
                .referenceId(postId)
                .referenceType(request.referenceType())
                .parentId(null)
                .content(request.content())
                .build();
        return commentRepository.save(comment).getId();
    }

    @Transactional
    public Long reply(Long postId, Long parentCommentId, CommentCreateRequest request, Long userId) {
        Comment parent = findCommentOrThrow(parentCommentId);

        if (parent.getParentId() != null) {
            throw new CustomException(CommentErrorCode.COMMENT_REPLY_DEPTH_EXCEEDED);
        }
        if (!parent.getReferenceId().equals(postId) || parent.getReferenceType() != request.referenceType()) {
            throw new CustomException(CommentErrorCode.COMMENT_REFERENCE_MISMATCH);
        }

        Comment reply = Comment.builder()
                .userId(userId)
                .referenceId(postId)
                .referenceType(request.referenceType())
                .parentId(parentCommentId)
                .content(request.content())
                .build();
        return commentRepository.save(reply).getId();
    }

    @Transactional
    public Long update(Long commentId, CommentUpdateRequest request, Long userId) {
        Comment comment = findCommentOrThrow(commentId);
        validateOwner(comment, userId);
        comment.update(request.content());
        return comment.getId();
    }

    @Transactional
    public Long delete(Long commentId, Long userId) {
        Comment comment = findCommentOrThrow(commentId);
        validateOwner(comment, userId);
        commentRepository.delete(comment);
        return comment.getId();
    }

    public List<CommentResponse> list(Long postId, ReferenceType referenceType) {
        return commentRepository
                .findByReferenceIdAndReferenceTypeOrderByCreatedAtAsc(postId, referenceType)
                .stream()
                .map(CommentResponse::from)
                .toList();
    }

    private Comment findCommentOrThrow(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(CommentErrorCode.COMMENT_NOT_FOUND));
    }

    private void validateOwner(Comment comment, Long userId) {
        if (!comment.getUserId().equals(userId)) {
            throw new CustomException(CommentErrorCode.COMMENT_ACCESS_DENIED);
        }
    }
}
