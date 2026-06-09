package com.chaewookim.accountbookformoms.domain.grouppruchase.dao;

import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.GroupPurchaseApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupPurchaseApplicationRepository extends JpaRepository<GroupPurchaseApplication, Long> {
    List<GroupPurchaseApplication> findByUserId(Long userId);
}
