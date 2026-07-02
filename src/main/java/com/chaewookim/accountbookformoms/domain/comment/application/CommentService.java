package com.chaewookim.accountbookformoms.domain.comment.application;

import com.chaewookim.accountbookformoms.domain.board.dao.BoardRepository;
import com.chaewookim.accountbookformoms.domain.board.entity.Board;
import com.chaewookim.accountbookformoms.domain.board.error.BoardErrorCode;
import com.chaewookim.accountbookformoms.domain.comment.dao.CommentRepository;
import com.chaewookim.accountbookformoms.domain.comment.dto.request.CommentCreateRequest;
import com.chaewookim.accountbookformoms.domain.comment.dto.request.CommentUpdateRequest;
import com.chaewookim.accountbookformoms.domain.comment.dto.response.CommentResponse;
import com.chaewookim.accountbookformoms.domain.comment.entity.Comment;
import com.chaewookim.accountbookformoms.domain.comment.enums.ReferenceType;
import com.chaewookim.accountbookformoms.domain.comment.error.CommentErrorCode;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.GroupPurchaseRepository;
import com.chaewookim.accountbookformoms.domain.like.application.PostLikeService;
import com.chaewookim.accountbookformoms.domain.like.enums.LikeTargetType;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final BoardRepository boardRepository;
    private final GroupPurchaseRepository groupPurchaseRepository;
    private final UserRepository userRepository;
    private final PostLikeService likeService;

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
        likeService.evictCount(
                com.chaewookim.accountbookformoms.domain.like.enums.LikeTargetType.COMMENT,
                comment.getId());
        return comment.getId();
    }

    @Transactional
    public Long acceptAnswer(Long commentId, Long requesterId) {
        Comment comment = findCommentOrThrow(commentId);
        if (comment.getReferenceType() != ReferenceType.QNA) {
            throw new CustomException(CommentErrorCode.COMMENT_ACCEPT_NOT_QNA);
        }
        if (comment.getParentId() != null) {
            throw new CustomException(CommentErrorCode.COMMENT_ACCEPT_REPLY_NOT_ALLOWED);
        }
        Board board = boardRepository.findById(comment.getReferenceId())
                .orElseThrow(() -> new CustomException(BoardErrorCode.BOARD_NOT_FOUND));
        if (!board.getUserId().equals(requesterId)) {
            throw new CustomException(BoardErrorCode.BOARD_ACCESS_DENIED);
        }

        List<Comment> siblings = commentRepository
                .findByReferenceIdAndReferenceTypeOrderByCreatedAtAsc(
                        comment.getReferenceId(), ReferenceType.QNA);
        for (Comment sibling : siblings) {
            if (sibling.getParentId() == null && sibling.isAccepted() && !sibling.getId().equals(commentId)) {
                sibling.setAccepted(false);
            }
        }
        comment.setAccepted(true);
        board.setResolved(true);
        return comment.getId();
    }

    public List<CommentResponse> list(Long postId, ReferenceType referenceType, Long viewerId) {
        List<Comment> comments = commentRepository
                .findByReferenceIdAndReferenceTypeOrderByCreatedAtAsc(postId, referenceType);
        return enrich(comments, viewerId);
    }

    public org.springframework.data.domain.Page<com.chaewookim.accountbookformoms.domain.comment.dto.response.CommentThreadResponse> listThreads(
            Long postId,
            ReferenceType referenceType,
            org.springframework.data.domain.Pageable pageable,
            Long viewerId
    ) {
        org.springframework.data.domain.Page<Comment> topLevel = commentRepository
                .findByReferenceIdAndReferenceTypeAndParentIdIsNullOrderByCreatedAtAsc(postId, referenceType, pageable);
        List<Long> parentIds = topLevel.getContent().stream().map(Comment::getId).toList();
        List<Comment> replies = parentIds.isEmpty()
                ? List.of()
                : commentRepository.findByParentIdInOrderByCreatedAtAsc(parentIds);
        List<Comment> all = new java.util.ArrayList<>(topLevel.getContent());
        all.addAll(replies);
        Map<Long, CommentResponse> byId = enrich(all, viewerId).stream()
                .collect(Collectors.toMap(CommentResponse::id, r -> r));
        Map<Long, List<CommentResponse>> repliesByParent = new java.util.HashMap<>();
        replies.forEach(r -> repliesByParent
                .computeIfAbsent(r.getParentId(), k -> new java.util.ArrayList<>())
                .add(byId.get(r.getId())));
        return topLevel.map(parent -> new com.chaewookim.accountbookformoms.domain.comment.dto.response.CommentThreadResponse(
                byId.get(parent.getId()),
                repliesByParent.getOrDefault(parent.getId(), List.of())
        ));
    }

    private List<CommentResponse> enrich(List<Comment> comments, Long viewerId) {
        List<Long> ids = comments.stream().map(Comment::getId).toList();
        Map<Long, String> nicknames = loadNicknames(comments.stream().map(Comment::getUserId).toList());
        Map<Long, Long> likeCounts = likeService.countByTargets(LikeTargetType.COMMENT, ids);
        Set<Long> liked = likeService.likedTargets(LikeTargetType.COMMENT, ids, viewerId);
        return comments.stream()
                .map(c -> CommentResponse.from(
                        c,
                        nicknames.get(c.getUserId()),
                        likeCounts.getOrDefault(c.getId(), 0L),
                        liked.contains(c.getId())))
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
