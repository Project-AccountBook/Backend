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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostLikeServiceTest {

    @Mock private PostLikeRepository likeRepository;
    @Mock private BoardRepository boardRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private LikeCountCacheService likeCountCache;

    @InjectMocks
    private PostLikeService postLikeService;

    private static final Long USER_ID = 1L;
    private static final Long TARGET_ID = 100L;

    @Nested
    @DisplayName("좋아요 토글")
    class Toggle {

        @Test
        @DisplayName("성공 — 좋아요 없던 상태에서 like 저장 + cache +1")
        void toggle_add() {
            given(boardRepository.existsById(TARGET_ID)).willReturn(true);
            given(likeRepository.findByUserIdAndTargetIdAndTargetType(USER_ID, TARGET_ID, LikeTargetType.BOARD))
                    .willReturn(Optional.empty());
            given(likeCountCache.getCount(LikeTargetType.BOARD, TARGET_ID)).willReturn(1L);

            LikeToggleResponse response = postLikeService.toggle(LikeTargetType.BOARD, TARGET_ID, USER_ID);

            assertThat(response.liked()).isTrue();
            assertThat(response.likeCount()).isEqualTo(1L);
            verify(likeRepository).save(any(PostLike.class));
            verify(likeCountCache).applyDelta(LikeTargetType.BOARD, TARGET_ID, 1L);
        }

        @Test
        @DisplayName("성공 — 좋아요 있던 상태에서 삭제 + cache -1")
        void toggle_remove() {
            PostLike existing = PostLike.builder().userId(USER_ID).targetId(TARGET_ID).targetType(LikeTargetType.BOARD).build();
            given(boardRepository.existsById(TARGET_ID)).willReturn(true);
            given(likeRepository.findByUserIdAndTargetIdAndTargetType(USER_ID, TARGET_ID, LikeTargetType.BOARD))
                    .willReturn(Optional.of(existing));
            given(likeCountCache.getCount(LikeTargetType.BOARD, TARGET_ID)).willReturn(0L);

            LikeToggleResponse response = postLikeService.toggle(LikeTargetType.BOARD, TARGET_ID, USER_ID);

            assertThat(response.liked()).isFalse();
            assertThat(response.likeCount()).isZero();
            verify(likeRepository).delete(existing);
            verify(likeCountCache).applyDelta(LikeTargetType.BOARD, TARGET_ID, -1L);
        }

        @Test
        @DisplayName("실패 — BOARD 대상이 존재하지 않으면 BOARD_NOT_FOUND")
        void toggle_fail_board_not_found() {
            given(boardRepository.existsById(TARGET_ID)).willReturn(false);

            assertThatThrownBy(() -> postLikeService.toggle(LikeTargetType.BOARD, TARGET_ID, USER_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BoardErrorCode.BOARD_NOT_FOUND);
            verify(likeRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패 — COMMENT 대상이 존재하지 않으면 COMMENT_NOT_FOUND")
        void toggle_fail_comment_not_found() {
            given(commentRepository.existsById(TARGET_ID)).willReturn(false);

            assertThatThrownBy(() -> postLikeService.toggle(LikeTargetType.COMMENT, TARGET_ID, USER_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommentErrorCode.COMMENT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("좋아요 여부 조회")
    class IsLiked {

        @Test
        @DisplayName("viewerId 가 null 이면 항상 false 반환 (DB 조회 없음)")
        void isLiked_null_viewer() {
            assertThat(postLikeService.isLiked(LikeTargetType.BOARD, TARGET_ID, null)).isFalse();
            verify(likeRepository, never()).findByUserIdAndTargetIdAndTargetType(any(), any(), any());
        }

        @Test
        @DisplayName("DB에 like 존재하면 true")
        void isLiked_true() {
            PostLike like = PostLike.builder().userId(USER_ID).targetId(TARGET_ID).targetType(LikeTargetType.BOARD).build();
            given(likeRepository.findByUserIdAndTargetIdAndTargetType(USER_ID, TARGET_ID, LikeTargetType.BOARD))
                    .willReturn(Optional.of(like));

            assertThat(postLikeService.isLiked(LikeTargetType.BOARD, TARGET_ID, USER_ID)).isTrue();
        }
    }

    @Test
    @DisplayName("evictCount — 캐시 서비스 evict 위임")
    void evictCount_delegates() {
        postLikeService.evictCount(LikeTargetType.BOARD, TARGET_ID);
        verify(likeCountCache).evict(eq(LikeTargetType.BOARD), eq(TARGET_ID));
    }
}
