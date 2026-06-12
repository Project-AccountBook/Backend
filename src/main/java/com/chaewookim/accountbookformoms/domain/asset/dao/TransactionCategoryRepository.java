package com.chaewookim.accountbookformoms.domain.asset.dao;

import com.chaewookim.accountbookformoms.domain.asset.entity.TransactionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionCategoryRepository extends JpaRepository<TransactionCategory, Long> {

    @Query("SELECT c FROM TransactionCategory c WHERE c.user.id = :userId OR c.user IS NULL")
    List<TransactionCategory> findAllByUserOrSystem(@Param("userId") Long userId);

    @Query("SELECT c.id FROM TransactionCategory c WHERE c.user IS NULL")
    List<Long> findSystemCategoryIds();
}
