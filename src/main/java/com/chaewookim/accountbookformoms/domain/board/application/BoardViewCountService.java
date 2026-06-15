package com.chaewookim.accountbookformoms.domain.board.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardViewCountService {

    private static final String VIEW_KEY_PREFIX = "board:views:";
    private static final String DIRTY_SET_KEY = "board:views:dirty";

    private final RedisTemplate<String, String> redisTemplate;

    public long increment(Long boardId) {
        Long delta = redisTemplate.opsForValue().increment(viewKey(boardId));
        redisTemplate.opsForSet().add(DIRTY_SET_KEY, String.valueOf(boardId));
        return delta == null ? 0L : delta;
    }

    public Map<Long, Long> getPendingDeltas(Collection<Long> boardIds) {
        if (boardIds == null || boardIds.isEmpty()) return Map.of();
        List<String> keys = boardIds.stream().map(this::viewKey).toList();
        List<String> values = redisTemplate.opsForValue().multiGet(keys);
        Map<Long, Long> result = new HashMap<>();
        int i = 0;
        for (Long id : boardIds) {
            String v = (values != null && i < values.size()) ? values.get(i) : null;
            result.put(id, v == null ? 0L : Long.parseLong(v));
            i++;
        }
        return result;
    }

    public Set<Long> snapshotDirtyBoardIds() {
        Set<String> raw = redisTemplate.opsForSet().members(DIRTY_SET_KEY);
        if (raw == null || raw.isEmpty()) return Collections.emptySet();
        return raw.stream().map(Long::parseLong).collect(Collectors.toSet());
    }

    // SREM 먼저 → GETSET 0 순서로 race 방지: SREM 이후 들어온 INCR 은 set 에 자기 자신을
    // 재등록하므로 다음 사이클에서 처리됨. delta 누락 없음.
    public long readAndReset(Long boardId) {
        redisTemplate.opsForSet().remove(DIRTY_SET_KEY, String.valueOf(boardId));
        String previous = redisTemplate.opsForValue().getAndSet(viewKey(boardId), "0");
        return previous == null ? 0L : Long.parseLong(previous);
    }

    private String viewKey(Long boardId) {
        return VIEW_KEY_PREFIX + boardId;
    }
}
