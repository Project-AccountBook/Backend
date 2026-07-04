package com.chaewookim.accountbookformoms.domain.like.application;

import com.chaewookim.accountbookformoms.domain.like.dao.PostLikeRepository;
import com.chaewookim.accountbookformoms.domain.like.enums.LikeTargetType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 좋아요 카운터 캐시 정합성 검증 배치.
 * <p>Redis 의 {@code like:count:*} 키를 SCAN 순회하며 캐시 값과 DB 실제 COUNT 를 비교, drift 를 로그로 남기고
 * DB 정본으로 캐시 값을 덮어쓴다. Cache-aside 패턴은 캐시 hit 상태에서의 drift 를 자가 감지할 수 없어
 * 주기적 검증이 필요.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LikeCountReconcileService {

    private static final String KEY_PATTERN = "like:count:*";
    private static final int SCAN_BATCH = 200;

    private final RedisTemplate<String, String> redisTemplate;
    private final PostLikeRepository likeRepository;

    public record ReconcileReport(int scanned, int mismatched, int corrected) {
    }

    /**
     * 캐시된 좋아요 카운터를 최대 {@code maxKeys} 개까지 검증한다.
     *
     * @param maxKeys 이번 실행에서 검사할 키의 상한. 0 이하이면 무제한.
     */
    public ReconcileReport reconcile(int maxKeys) {
        long start = System.currentTimeMillis();
        Map<LikeTargetType, Map<Long, Long>> cachedByType = new EnumMap<>(LikeTargetType.class);
        int scanned = 0;

        ScanOptions options = ScanOptions.scanOptions().match(KEY_PATTERN).count(SCAN_BATCH).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                if (maxKeys > 0 && scanned >= maxKeys) break;
                String key = cursor.next();
                ParsedKey parsed = parse(key);
                if (parsed == null) continue;
                String v = redisTemplate.opsForValue().get(key);
                if (v == null) continue; // 이번 사이클 동안 만료됐거나 evict — 무시
                long cached;
                try {
                    cached = Long.parseLong(v);
                } catch (NumberFormatException e) {
                    log.warn("like count key {} 손상됨: value={}", key, v);
                    redisTemplate.delete(key);
                    continue;
                }
                cachedByType
                        .computeIfAbsent(parsed.type, t -> new HashMap<>())
                        .put(parsed.id, cached);
                scanned++;
            }
        }

        int mismatched = 0;
        int corrected = 0;
        for (Map.Entry<LikeTargetType, Map<Long, Long>> entry : cachedByType.entrySet()) {
            LikeTargetType type = entry.getKey();
            Map<Long, Long> cached = entry.getValue();
            if (cached.isEmpty()) continue;

            // 벌크 DB COUNT
            Map<Long, Long> dbCounts = new HashMap<>();
            likeRepository.countByTargets(type, new ArrayList<>(cached.keySet()))
                    .forEach(row -> dbCounts.put(row.getTargetId(), row.getCnt()));

            for (Map.Entry<Long, Long> e : cached.entrySet()) {
                Long id = e.getKey();
                long cachedValue = e.getValue();
                long dbValue = dbCounts.getOrDefault(id, 0L);
                if (cachedValue != dbValue) {
                    mismatched++;
                    log.warn("like count drift 감지: type={}, id={}, cache={}, db={} → db 값으로 정정",
                            type, id, cachedValue, dbValue);
                    // 재적재 (SET). TTL 은 LikeCountCacheService.TTL(30분)에 맞추지 않고 default 로 두어도
                    // 다음 read 가 TTL 재갱신하니 큰 문제 없음.
                    redisTemplate.opsForValue().set(cacheKey(type, id), String.valueOf(dbValue));
                    corrected++;
                }
            }
        }

        log.info("like count reconcile: scanned={}, mismatched={}, corrected={}, {}ms",
                scanned, mismatched, corrected, System.currentTimeMillis() - start);
        return new ReconcileReport(scanned, mismatched, corrected);
    }

    private ParsedKey parse(String key) {
        // like:count:{TYPE}:{ID}
        String[] parts = key.split(":");
        if (parts.length != 4) return null;
        try {
            LikeTargetType type = LikeTargetType.valueOf(parts[2]);
            long id = Long.parseLong(parts[3]);
            return new ParsedKey(type, id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String cacheKey(LikeTargetType type, Long id) {
        return "like:count:" + type.name() + ":" + id;
    }

    private record ParsedKey(LikeTargetType type, long id) {
    }
}
