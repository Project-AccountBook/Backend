package com.chaewookim.accountbookformoms.domain.grouppruchase.dao;

import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.GroupPurchase;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.PurchaseStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupPurchaseRepository extends JpaRepository<GroupPurchase, Long> {

    @Query("SELECT gp FROM GroupPurchase gp " +
           "WHERE gp.status = :status " +
           "AND (:region IS NULL OR gp.pickupLocation LIKE %:region%)")
    List<GroupPurchase> findActiveGroupPurchases(
            @Param("status") PurchaseStatus status,
            @Param("region") String region,
            Sort sort
    );
}
