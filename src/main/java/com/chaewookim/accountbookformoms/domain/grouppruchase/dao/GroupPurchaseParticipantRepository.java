package com.chaewookim.accountbookformoms.domain.grouppruchase.dao;

import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.GroupPurchaseParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupPurchaseParticipantRepository extends JpaRepository<GroupPurchaseParticipant, Long> {
    
    Optional<GroupPurchaseParticipant> findByGroupPurchaseIdAndUserId(Long groupPurchaseId, Long userId);
    
    boolean existsByGroupPurchaseIdAndUserId(Long groupPurchaseId, Long userId);
    
    List<GroupPurchaseParticipant> findByGroupPurchaseId(Long groupPurchaseId);
}
