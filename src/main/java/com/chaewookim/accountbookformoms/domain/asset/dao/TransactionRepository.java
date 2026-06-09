package com.chaewookim.accountbookformoms.domain.asset.dao;

import com.chaewookim.accountbookformoms.domain.asset.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findAllByUserIdAndAccountIdAndTransactionDateBetween(Long userId, Long accountId, LocalDate startDate, LocalDate endDate);
}
