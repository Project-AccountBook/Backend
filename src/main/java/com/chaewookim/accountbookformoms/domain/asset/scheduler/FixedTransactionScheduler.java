package com.chaewookim.accountbookformoms.domain.asset.scheduler;

import com.chaewookim.accountbookformoms.domain.asset.application.TransactionService;
import com.chaewookim.accountbookformoms.domain.asset.dao.FixedTransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.dto.request.TransactionRequest;
import com.chaewookim.accountbookformoms.domain.asset.entity.FixedTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FixedTransactionScheduler {

    private final FixedTransactionRepository fixedTransactionRepository;
    private final TransactionService transactionService;

    @Scheduled(cron = "0 0 0 * * *")
    @SchedulerLock(name = "FixedTransactionScheduler_processFixedTransactions",
            lockAtMostFor = "PT1H", lockAtLeastFor = "PT1M")
    @Transactional
    public void processFixedTransactions() {

        log.info("고정 거래 스케줄러 시작: {}", LocalDate.now());
        List<FixedTransaction> targetTransactions = fixedTransactionRepository.findAllByIsActiveTrueAndNextExecutionDateLessThanEqual(LocalDate.now());

        for (FixedTransaction fixedTransaction : targetTransactions) {

            LocalDate today = LocalDate.now();
            boolean alreadyExecuted = today.equals(fixedTransaction.getLastExecutedDate());

            if (isTodayTargetDay(fixedTransaction, today) && !alreadyExecuted) {
                try {
                    TransactionRequest request = new TransactionRequest(
                            fixedTransaction.getAccount().getId(),
                            null,
                            fixedTransaction.getTransactionCategory().getId(),
                            fixedTransaction.getType(),
                            fixedTransaction.getAmount(),
                            today,
                            fixedTransaction.getDescription()
                    );

                    transactionService.createTransaction(fixedTransaction.getUser().getId(), request);
                    fixedTransaction.updateExecutionStatus(today);

                    log.info("고정 거래 생성 완료: ID={}", fixedTransaction.getId());
                } catch (Exception e) {
                    log.error("고정 거래 생성 실패: ID={}, error={}", fixedTransaction.getId(), e.getMessage());
                }
            }
        }
    }

    private boolean isTodayTargetDay(FixedTransaction fixedTransaction, LocalDate today) {

        if (fixedTransaction.getEndDate() != null && today.isAfter(fixedTransaction.getEndDate())) return false;
        if (today.isBefore(fixedTransaction.getStartDate())) return false;

        return today.getDayOfMonth() == fixedTransaction.getRepeatDay();
    }
}
