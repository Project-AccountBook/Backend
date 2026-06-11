package com.chaewookim.accountbookformoms.domain.grouppruchase.dao;

import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    Optional<Wishlist> findByUserIdAndGroupPurchaseId(Long userId, Long groupPurchaseId);
}
