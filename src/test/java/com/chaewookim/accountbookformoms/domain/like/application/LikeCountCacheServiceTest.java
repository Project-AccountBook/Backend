package com.chaewookim.accountbookformoms.domain.like.application;

import com.chaewookim.accountbookformoms.domain.like.dao.PostLikeRepository;
import com.chaewookim.accountbookformoms.domain.like.enums.LikeTargetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LikeCountCacheServiceTest {

    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private PostLikeRepository likeRepository;

    @InjectMocks
    private LikeCountCacheService likeCountCacheService;

    private static final String KEY_BOARD_1 = "like:count:BOARD:1";

    @Nested
    @DisplayName("getCount")
    class GetCount {

        @Test
        @DisplayName("Redis hit 시 DB 조회 없이 값 반환")
        void hit() {
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(valueOps.get(KEY_BOARD_1)).willReturn("42");

            long result = likeCountCacheService.getCount(LikeTargetType.BOARD, 1L);

            assertThat(result).isEqualTo(42L);
            verify(likeRepository, never()).countByTargetIdAndTargetType(any(), any());
        }

        @Test
        @DisplayName("Redis miss 시 DB COUNT 후 캐시에 SET (TTL 포함)")
        void miss_fills_from_db() {
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(valueOps.get(KEY_BOARD_1)).willReturn(null);
            given(likeRepository.countByTargetIdAndTargetType(1L, LikeTargetType.BOARD)).willReturn(7L);

            long result = likeCountCacheService.getCount(LikeTargetType.BOARD, 1L);

            assertThat(result).isEqualTo(7L);
            verify(valueOps).set(eq(KEY_BOARD_1), eq("7"), any(Duration.class));
        }
    }

    @Nested
    @DisplayName("applyDelta")
    class ApplyDelta {

        @Test
        @DisplayName("캐시 키 존재 시 INCR/DECR 원자적 증감")
        void increments_when_present() {
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(redisTemplate.hasKey(KEY_BOARD_1)).willReturn(true);

            likeCountCacheService.applyDelta(LikeTargetType.BOARD, 1L, 1L);

            verify(valueOps).increment(KEY_BOARD_1, 1L);
        }

        @Test
        @DisplayName("캐시 키 없으면 INCR 실행하지 않음 (drift 방지)")
        void skips_when_absent() {
            given(redisTemplate.hasKey(KEY_BOARD_1)).willReturn(false);

            likeCountCacheService.applyDelta(LikeTargetType.BOARD, 1L, 1L);

            verify(redisTemplate, never()).opsForValue();
        }
    }

    @Nested
    @DisplayName("getCounts")
    class GetCounts {

        @Test
        @DisplayName("전체 hit 이면 DB 미조회")
        void all_hit() {
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(valueOps.multiGet(any())).willReturn(List.of("3", "5"));

            Map<Long, Long> result = likeCountCacheService.getCounts(LikeTargetType.BOARD, List.of(1L, 2L));

            assertThat(result).containsEntry(1L, 3L).containsEntry(2L, 5L);
            verify(likeRepository, never()).countByTargets(any(), any());
        }

        @Test
        @DisplayName("빈 collection 이면 빈 map 즉시 반환")
        void empty_input() {
            Map<Long, Long> result = likeCountCacheService.getCounts(LikeTargetType.BOARD, List.of());

            assertThat(result).isEmpty();
            verify(redisTemplate, never()).opsForValue();
        }
    }

    @Test
    @DisplayName("evict — DEL 실행")
    void evict() {
        likeCountCacheService.evict(LikeTargetType.BOARD, 1L);
        verify(redisTemplate).delete(KEY_BOARD_1);
    }
}
