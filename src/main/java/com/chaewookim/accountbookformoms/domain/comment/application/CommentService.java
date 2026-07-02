package com.chaewookim.accountbookformoms.domain.comment.application;

import com.chaewookim.accountbookformoms.domain.board.dao.BoardRepository;
import com.chaewookim.accountbookformoms.domain.board.error.BoardErrorCode;
import com.chaewookim.accountbookformoms.domain.comment.dao.CommentRepository;
import com.chaewookim.accountbookformoms.domain.comment.dto.request.CommentCreateRequest;
import com.chaewookim.accountbookformoms.domain.comment.dto.request.CommentUpdateRequest;
import com.chaewookim.accountbookformoms.domain.comment.dto.response.CommentResponse;
import com.chaewookim.accountbookformoms.domain.comment.entity.Comment;
import com.chaewookim.accountbookformoms.domain.comment.enums.ReferenceType;
import com.chaewookim.accountbookformoms.domain.comment.error.CommentErrorCode;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.GroupPurchaseRepository;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final BoardRepository boardRepository;
    private final GroupPurchaseRepository groupPurchaseRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long create(Long postId, CommentCreateRequest request, Long userId) {
        validatePostExists(postId, request.referenceType());
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
        validatePostExists(postId, request.referenceType());
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
        List<Comment> comments = commentRepository
                .findByReferenceIdAndReferenceTypeOrderByCreatedAtAsc(postId, referenceType);
        Map<Long, String> nicknames = loadNicknames(comments.stream().map(Comment::getUserId).toList());
        return comments.stream()
                .map(c -> CommentResponse.from(c, nicknames.get(c.getUserId())))
                .toList();
    }

    private Map<Long, String> loadNicknames(List<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
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

    private void validatePostExists(Long postId, ReferenceType referenceType) {
        if (referenceType == ReferenceType.QNA || referenceType == ReferenceType.KNOWHOW) {
            if (!boardRepository.existsById(postId)) {
                throw new CustomException(BoardErrorCode.BOARD_NOT_FOUND);
            }
        } else if (referenceType == ReferenceType.GROUPPURCHASE) {
            if (!groupPurchaseRepository.existsById(postId)) {
                throw new CustomException(ErrorCode.GROUP_PURCHASE_NOT_FOUND);
            }
        }
    }
}
