package com.chaewookim.accountbookformoms.domain.user.dao;

import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.Category;
import com.chaewookim.accountbookformoms.domain.user.entity.InterestCategory;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterestCategoryRepository extends JpaRepository<InterestCategory, Long> {

    List<InterestCategory> findByCategoryAndIsAlarmEnabledTrue(Category category);
    List<InterestCategory> findByUserId(Long userId);
    boolean existsByUserAndCategory(User user, Category category);
    Optional<InterestCategory> findByIdAndUserId(Long id, Long userId);
}
