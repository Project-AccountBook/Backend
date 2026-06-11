package com.chaewookim.accountbookformoms.domain.grouppruchase.dao;

import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.GroupPurchase;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.PurchaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GroupPurchaseRepository extends JpaRepository<GroupPurchase, Long> {

    @Query("SELECT gp FROM GroupPurchase gp " +
           "WHERE gp.status = :status " +
           "AND (:region IS NULL OR gp.pickupLocation LIKE %:region%) " +
           "AND (:categoryId IS NULL OR gp.categoryId = :categoryId)")
    List<GroupPurchase> findActiveGroupPurchases(
            @Param("status") PurchaseStatus status,
            @Param("region") String region,
            @Param("categoryId") Long categoryId,
            Sort sort
    );

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(gp.currentParticipants), 0) FROM GroupPurchase gp WHERE gp.status = :status")
    long sumCurrentParticipantsByStatus(@Param("status") PurchaseStatus status);

    long countByStatus(PurchaseStatus status);

    @Query("SELECT gp FROM GroupPurchase gp " +
           "WHERE (:status IS NULL OR " +
           "  (:status = 'RECRUITING' AND gp.status = 'RECRUITING') OR " +
           "  (:status = 'SUCCESS' AND (gp.status = 'SUCCESS' OR gp.status = 'CLOSED')) OR " +
           "  (:status = 'FAILED' AND gp.status = 'FAILED') OR " +
           "  (:status = 'REPORTED' AND (SELECT COUNT(r) FROM Report r WHERE r.targetType = 'GROUP_PURCHASE' AND r.targetId = gp.id) > 0)" +
           ")")
    Page<GroupPurchase> findAllForAdmin(
            @Param("status") String status,
            Pageable pageable
    );
}

