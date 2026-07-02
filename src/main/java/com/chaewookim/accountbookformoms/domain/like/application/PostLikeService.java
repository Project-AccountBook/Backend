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
    private final LikeCountCacheService likeCountCache;

    @Transactional
    public LikeToggleResponse toggle(LikeTargetType type, Long targetId, Long userId) {
        validateTargetExists(type, targetId);
        Optional<PostLike> existing = likeRepository
                .findByUserIdAndTargetIdAndTargetType(userId, targetId, type);
        boolean nowLiked;
        long delta;
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            nowLiked = false;
            delta = -1L;
        } else {
            likeRepository.save(PostLike.builder()
                    .userId(userId)
                    .targetId(targetId)
                    .targetType(type)
                    .build());
            nowLiked = true;
            delta = 1L;
        }
        // 캐시가 있을 때만 원자적 증감. 미스면 다음 read 가 DB 정본 기준으로 재적재.
        likeCountCache.applyDelta(type, targetId, delta);
        long count = likeCountCache.getCount(type, targetId);
        return new LikeToggleResponse(nowLiked, count);
    }

    public Map<Long, Long> countByTargets(LikeTargetType type, Collection<Long> ids) {
        return likeCountCache.getCounts(type, ids);
    }

    public long count(LikeTargetType type, Long targetId) {
        return likeCountCache.getCount(type, targetId);
    }

    /** 게시물/댓글 삭제 시 좋아요 카운터 캐시 정리. */
    public void evictCount(LikeTargetType type, Long targetId) {
        likeCountCache.evict(type, targetId);
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
