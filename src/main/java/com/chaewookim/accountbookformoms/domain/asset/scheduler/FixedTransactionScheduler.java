package com.chaewookim.accountbookformoms.domain.asset.scheduler;

import com.chaewookim.accountbookformoms.domain.asset.application.FixedTransactionExecutor;
import com.chaewookim.accountbookformoms.domain.asset.dao.FixedTransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.entity.FixedTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class FixedTransactionScheduler {

    private final FixedTransactionRepository fixedTransactionRepository;
    private final FixedTransactionExecutor fixedTransactionExecutor;

    @Scheduled(cron = "5 0 0 * * *")
    @SchedulerLock(name = "FixedTransactionScheduler_processFixedTransactions",
            lockAtMostFor = "PT1H", lockAtLeastFor = "PT1M")
    @Transactional
    public void processFixedTransactions() {

        LocalDate today = LocalDate.now();
        log.info("고정 거래 스케줄러 시작: {}", today);
        List<FixedTransaction> targetTransactions = new ArrayList<>(
                fixedTransactionRepository.findAllByIsActiveTrueAndNextExecutionDateLessThanEqual(today)
        );
        Set<Long> seenIds = new HashSet<>();
        targetTransactions.forEach(ft -> seenIds.add(ft.getId()));

        for (FixedTransaction candidate : fixedTransactionRepository.findAllByIsActiveTrue()) {
            if (seenIds.contains(candidate.getId())) {
                continue;
            }
            if (candidate.isExecutionDay(today) && !today.equals(candidate.getLastExecutedDate())) {
                candidate.alignNextExecutionDateIfStale(today);
                targetTransactions.add(candidate);
                seenIds.add(candidate.getId());
            }
        }

        for (FixedTransaction fixedTransaction : targetTransactions) {
            try {
                fixedTransactionExecutor.executeIfDue(fixedTransaction, today);
            } catch (Exception e) {
                log.error("고정 거래 생성 실패: ID={}, error={}", fixedTransaction.getId(), e.getMessage());
            }
        }
    }
}
