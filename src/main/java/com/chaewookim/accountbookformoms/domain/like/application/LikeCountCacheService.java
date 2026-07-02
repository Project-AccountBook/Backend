package com.chaewookim.accountbookformoms.domain.like.application;

import com.chaewookim.accountbookformoms.domain.like.dao.PostLikeRepository;
import com.chaewookim.accountbookformoms.domain.like.enums.LikeTargetType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 좋아요 카운터 Redis 캐싱.
 * <p>Cache-aside + INCR/DECR 하이브리드:</p>
 * <ul>
 *   <li>읽기: Redis GET/MGET → 미스면 DB COUNT 후 SET(TTL 30분)</li>
 *   <li>토글 write: DB mutation 이후 캐시가 존재할 때만 INCR/DECR (없으면 다음 읽기가 lazy fill).</li>
 * </ul>
 * <p>키가 없을 때 INCR 을 실행하지 않는 이유: 실제 DB count 가 0 이 아닐 수도 있어 counter 가 어긋남.
 * 따라서 존재 여부(EXISTS)를 먼저 확인하고 있을 때만 원자적 증감. 없으면 다음 read 가 DB 정본 기준으로 재적재.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LikeCountCacheService {

    private static final String KEY_PREFIX = "like:count:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, String> redisTemplate;
    private final PostLikeRepository likeRepository;

    public long getCount(LikeTargetType type, Long targetId) {
        String key = key(type, targetId);
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return parseSafe(cached);
        }
        long dbCount = likeRepository.countByTargetIdAndTargetType(targetId, type);
        redisTemplate.opsForValue().set(key, String.valueOf(dbCount), TTL);
        return dbCount;
    }

    public Map<Long, Long> getCounts(LikeTargetType type, Collection<Long> targetIds) {
        if (targetIds == null || targetIds.isEmpty()) return Map.of();
        List<Long> ordered = targetIds.stream().distinct().toList();

        List<String> keys = ordered.stream().map(id -> key(type, id)).toList();
        List<String> cached = redisTemplate.opsForValue().multiGet(keys);

        Map<Long, Long> result = new HashMap<>();
        List<Long> missing = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            String v = cached != null && i < cached.size() ? cached.get(i) : null;
            if (v == null) {
                missing.add(ordered.get(i));
            } else {
                result.put(ordered.get(i), parseSafe(v));
            }
        }

        if (!missing.isEmpty()) {
            Map<Long, Long> loaded = new HashMap<>();
            likeRepository.countByTargets(type, missing)
                    .forEach(row -> loaded.put(row.getTargetId(), row.getCnt()));
            for (Long id : missing) {
                long cnt = loaded.getOrDefault(id, 0L);
                result.put(id, cnt);
                redisTemplate.opsForValue().set(key(type, id), String.valueOf(cnt), TTL);
            }
        }
        return result;
    }

    /** DB 토글 이후 캐시 반영. 캐시가 있을 때만 원자적 증감. */
    public void applyDelta(LikeTargetType type, Long targetId, long delta) {
        String key = key(type, targetId);
        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            redisTemplate.opsForValue().increment(key, delta);
        }
    }

    /** 게시물/댓글 삭제 시 명시적 무효화. */
    public void evict(LikeTargetType type, Long targetId) {
        redisTemplate.delete(key(type, targetId));
    }

    private String key(LikeTargetType type, Long targetId) {
        return KEY_PREFIX + type.name() + ":" + targetId;
    }

    private long parseSafe(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("like count cache corrupted value: {}", value);
            return 0L;
        }
    }
}
