package com.chaewookim.accountbookformoms.domain.grouppruchase.dao;

import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.Report;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    @Query("SELECT r FROM Report r " +
           "WHERE (:isProcessed IS NULL OR r.isProcessed = :isProcessed) " +
           "AND (:targetType IS NULL OR r.targetType = :targetType)")
    Page<Report> findReports(
            @Param("isProcessed") Boolean isProcessed,
            @Param("targetType") ReportTargetType targetType,
            Pageable pageable
    );

    long countByTargetTypeAndTargetId(ReportTargetType targetType, Long targetId);
}

