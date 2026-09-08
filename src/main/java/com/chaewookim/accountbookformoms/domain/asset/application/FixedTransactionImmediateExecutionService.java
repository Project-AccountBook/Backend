package com.chaewookim.accountbookformoms.domain.asset.application;

import com.chaewookim.accountbookformoms.domain.asset.dao.FixedTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class FixedTransactionImmediateExecutionService {

    private final FixedTransactionRepository fixedTransactionRepository;
    private final FixedTransactionExecutor fixedTransactionExecutor;

    @Transactional
    public void executeIfDue(Long fixedTransactionId) {
        fixedTransactionRepository.findById(fixedTransactionId).ifPresent(fixedTransaction -> {
            try {
                fixedTransactionExecutor.executeIfDue(fixedTransaction, LocalDate.now());
            } catch (Exception e) {
                log.warn("고정 거래 즉시 실행 실패: ID={}, error={}", fixedTransactionId, e.getMessage());
            }
        });
    }
}
