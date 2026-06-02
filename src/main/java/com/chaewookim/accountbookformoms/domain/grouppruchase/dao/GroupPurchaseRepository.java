package com.chaewookim.accountbookformoms.domain.grouppruchase.dao;

import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.GroupPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupPurchaseRepository extends JpaRepository<GroupPurchase, Long> {
}
