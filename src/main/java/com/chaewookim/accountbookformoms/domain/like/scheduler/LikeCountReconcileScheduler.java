package com.chaewookim.accountbookformoms.domain.like.scheduler;

import com.chaewookim.accountbookformoms.domain.like.application.LikeCountReconcileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 좋아요 카운터 캐시 정합성 배치.
 * <p>매시 7분 실행 (트래픽 하락 시점 근처). ShedLock 으로 다중 인스턴스 중복 실행 차단.</p>
 * <p>1회 실행당 최대 {@value #MAX_KEYS_PER_RUN} 개 키를 검증. Redis SCAN 은 랜덤 순서라
 * 게시물 수가 늘어나도 시간이 지나면서 골고루 커버된다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.like.reconcile.enabled",
        havingValue = "true", matchIfMissing = true)
public class LikeCountReconcileScheduler {

    private static final int MAX_KEYS_PER_RUN = 500;

    private final LikeCountReconcileService reconcileService;

    @Scheduled(cron = "0 7 * * * *")
    @SchedulerLock(name = "LikeCountReconcileScheduler_reconcile",
            lockAtMostFor = "PT10M", lockAtLeastFor = "PT30S")
    public void reconcile() {
        reconcileService.reconcile(MAX_KEYS_PER_RUN);
    }
}
