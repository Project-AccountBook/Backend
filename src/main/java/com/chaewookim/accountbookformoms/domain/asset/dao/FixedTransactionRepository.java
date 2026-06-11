package com.chaewookim.accountbookformoms.domain.asset.dao;

import com.chaewookim.accountbookformoms.domain.asset.entity.FixedTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FixedTransactionRepository extends JpaRepository<FixedTransaction, Long> {

    List<FixedTransaction> findAllByUserId(Long userId);
    List<FixedTransaction> findAllByIsActiveTrueAndNextExecutionDateLessThanEqual(LocalDate today);
}
