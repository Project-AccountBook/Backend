package com.chaewookim.accountbookformoms.domain.board.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BoardViewCountServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private SetOperations<String, String> setOps;

    @InjectMocks
    private BoardViewCountService viewCountService;

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(redisTemplate.opsForSet()).willReturn(setOps);
    }

    @Test
    @DisplayName("increment — 카운터 INCR + dirty set SADD, 새 카운터 값 반환")
    void increment_addsToDirtySet() {
        given(valueOps.increment("board:views:100")).willReturn(4L);

        long result = viewCountService.increment(100L);

        assertThat(result).isEqualTo(4L);
        verify(valueOps).increment("board:views:100");
        verify(setOps).add("board:views:dirty", "100");
    }

    @Test
    @DisplayName("increment — INCR 결과 null 이면 0 반환")
    void increment_nullSafe() {
        given(valueOps.increment(any(String.class))).willReturn(null);

        long result = viewCountService.increment(100L);

        assertThat(result).isZero();
    }

    @Test
    @DisplayName("getPendingDeltas — MGET 결과 파싱, 없는 키(null)는 0")
    void getPendingDeltas_parsesValues() {
        given(valueOps.multiGet(List.of("board:views:1", "board:views:2")))
                .willReturn(java.util.Arrays.asList("5", null));

        Map<Long, Long> result = viewCountService.getPendingDeltas(List.of(1L, 2L));

        assertThat(result).containsEntry(1L, 5L).containsEntry(2L, 0L);
    }

    @Test
    @DisplayName("getPendingDeltas — 빈 입력은 빈 맵 반환 (Redis 호출 없음)")
    void getPendingDeltas_emptyInput() {
        Map<Long, Long> result = viewCountService.getPendingDeltas(List.of());

        assertThat(result).isEmpty();
        verify(valueOps, never()).multiGet(any());
    }

    @Test
    @DisplayName("snapshotDirtyBoardIds — dirty set 멤버를 Long 으로 파싱")
    void snapshotDirtyBoardIds_parsesLong() {
        given(setOps.members("board:views:dirty")).willReturn(Set.of("10", "20"));

        Set<Long> ids = viewCountService.snapshotDirtyBoardIds();

        assertThat(ids).containsExactlyInAnyOrder(10L, 20L);
    }

    @Test
    @DisplayName("snapshotDirtyBoardIds — dirty set 이 비어 있으면 빈 Set")
    void snapshotDirtyBoardIds_empty() {
        given(setOps.members("board:views:dirty")).willReturn(Set.of());

        assertThat(viewCountService.snapshotDirtyBoardIds()).isEmpty();
    }

    @Test
    @DisplayName("readAndReset — SREM 먼저 → GETSET 0 순서 보장, 이전 값 반환")
    void readAndReset_orderAndReturn() {
        given(valueOps.getAndSet("board:views:42", "0")).willReturn("9");

        long result = viewCountService.readAndReset(42L);

        assertThat(result).isEqualTo(9L);

        var order = inOrder(setOps, valueOps);
        order.verify(setOps).remove("board:views:dirty", "42");
        order.verify(valueOps).getAndSet(eq("board:views:42"), eq("0"));
    }

    @Test
    @DisplayName("readAndReset — 이전 값이 null 이면 0 반환")
    void readAndReset_nullPrevious() {
        given(valueOps.getAndSet(any(String.class), eq("0"))).willReturn(null);

        long result = viewCountService.readAndReset(42L);

        assertThat(result).isZero();
    }
}
