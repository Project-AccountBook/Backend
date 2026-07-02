package com.chaewookim.accountbookformoms.domain.like.application;

import com.chaewookim.accountbookformoms.domain.board.dao.BoardRepository;
import com.chaewookim.accountbookformoms.domain.board.error.BoardErrorCode;
import com.chaewookim.accountbookformoms.domain.comment.dao.CommentRepository;
import com.chaewookim.accountbookformoms.domain.comment.error.CommentErrorCode;
import com.chaewookim.accountbookformoms.domain.like.dao.PostLikeRepository;
import com.chaewookim.accountbookformoms.domain.like.dto.response.LikeToggleResponse;
import com.chaewookim.accountbookformoms.domain.like.entity.PostLike;
import com.chaewookim.accountbookformoms.domain.like.enums.LikeTargetType;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostLikeService {

    private final PostLikeRepository likeRepository;
    private final BoardRepository boardRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public LikeToggleResponse toggle(LikeTargetType type, Long targetId, Long userId) {
        validateTargetExists(type, targetId);
        Optional<PostLike> existing = likeRepository
                .findByUserIdAndTargetIdAndTargetType(userId, targetId, type);
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
        } else {
            likeRepository.save(PostLike.builder()
                    .userId(userId)
                    .targetId(targetId)
                    .targetType(type)
                    .build());
        }
        long count = likeRepository.countByTargetIdAndTargetType(targetId, type);
        return new LikeToggleResponse(existing.isEmpty(), count);
    }

    public Map<Long, Long> countByTargets(LikeTargetType type, Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        Map<Long, Long> result = new HashMap<>();
        likeRepository.countByTargets(type, ids)
                .forEach(row -> result.put(row.getTargetId(), row.getCnt()));
        return result;
    }

    public long count(LikeTargetType type, Long targetId) {
        return likeRepository.countByTargetIdAndTargetType(targetId, type);
    }

    public Set<Long> likedTargets(LikeTargetType type, Collection<Long> ids, Long userId) {
        if (userId == null || ids.isEmpty()) return Collections.emptySet();
        return likeRepository.findLikedTargets(userId, type, ids).stream()
                .collect(Collectors.toSet());
    }

    public boolean isLiked(LikeTargetType type, Long targetId, Long userId) {
        if (userId == null) return false;
        return likeRepository.findByUserIdAndTargetIdAndTargetType(userId, targetId, type).isPresent();
    }

    private void validateTargetExists(LikeTargetType type, Long targetId) {
        if (type == LikeTargetType.BOARD) {
            if (!boardRepository.existsById(targetId)) {
                throw new CustomException(BoardErrorCode.BOARD_NOT_FOUND);
            }
        } else if (type == LikeTargetType.COMMENT) {
            if (!commentRepository.existsById(targetId)) {
                throw new CustomException(CommentErrorCode.COMMENT_NOT_FOUND);
            }
        }
    }
}
