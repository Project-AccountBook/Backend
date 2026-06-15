package com.chaewookim.accountbookformoms.domain.board.scheduler;

import com.chaewookim.accountbookformoms.domain.board.application.BoardViewCountService;
import com.chaewookim.accountbookformoms.domain.board.dao.BoardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class BoardViewSyncScheduler {

    private final BoardViewCountService viewCountService;
    private final BoardRepository boardRepository;

    @Scheduled(cron = "0 */5 * * * *")
    @SchedulerLock(name = "BoardViewSyncScheduler_sync",
            lockAtMostFor = "PT5M", lockAtLeastFor = "PT30S")
    @Transactional
    public void sync() {
        Set<Long> ids = viewCountService.snapshotDirtyBoardIds();
        if (ids.isEmpty()) return;

        long start = System.currentTimeMillis();
        int applied = 0;
        long totalDelta = 0;

        for (Long id : ids) {
            long delta = viewCountService.readAndReset(id);
            if (delta <= 0) continue;
            int updated = boardRepository.increaseViews(id, delta);
            if (updated == 0) {
                log.warn("board views sync: board {} not found (delta {} lost)", id, delta);
            } else {
                applied++;
                totalDelta += delta;
            }
        }

        log.info("board views sync 완료: {} 보드 / {} views / {}ms",
                applied, totalDelta, System.currentTimeMillis() - start);
    }
}
