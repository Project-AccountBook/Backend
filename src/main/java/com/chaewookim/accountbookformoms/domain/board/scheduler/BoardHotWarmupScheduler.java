package com.chaewookim.accountbookformoms.domain.board.scheduler;

import com.chaewookim.accountbookformoms.domain.board.application.BoardService;
import com.chaewookim.accountbookformoms.domain.board.enums.BOARD_TYPE;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Board HOT 랭킹 캐시 warm-up.
 * <p>프론트가 가장 자주 호출하는 (type, days, limit) 조합을 미리 계산해 Redis 에 넣어둔다.
 * TTL 5분(RedisConfig.CACHE_BOARD_HOT)과 정합되도록 4분 주기 실행.</p>
 * <p>ShedLock 으로 다중 인스턴스 중복 실행 차단.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.cache.warmup.board-hot.enabled",
        havingValue = "true", matchIfMissing = true)
public class BoardHotWarmupScheduler {

    private static final int WARMUP_DAYS = 7;
    private static final int WARMUP_LIMIT = 3;

    private final BoardService boardService;

    @Scheduled(cron = "0 */4 * * * *")
    @SchedulerLock(name = "BoardHotWarmupScheduler_warmup",
            lockAtMostFor = "PT3M", lockAtLeastFor = "PT30S")
    public void warmup() {
        long start = System.currentTimeMillis();
        int warmed = 0;
        for (BOARD_TYPE type : BOARD_TYPE.values()) {
            try {
                boardService.hot(type, WARMUP_DAYS, WARMUP_LIMIT);
                warmed++;
            } catch (Exception e) {
                log.warn("board:hot warm-up 실패: type={}, error={}", type, e.getMessage());
            }
        }
        log.info("board:hot warm-up 완료: {} 항목 / {}ms",
                warmed, System.currentTimeMillis() - start);
    }
}
