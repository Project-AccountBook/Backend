package com.chaewookim.accountbookformoms.domain.user.dao;

import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.Category;
import com.chaewookim.accountbookformoms.domain.user.entity.InterestCategory;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InterestCategoryRepository extends JpaRepository<InterestCategory, Long> {

    List<InterestCategory> findByCategoryAndIsAlarmEnabledTrue(Category category);

    @Query("""
            SELECT ic FROM InterestCategory ic
            JOIN FETCH ic.user u
            LEFT JOIN FETCH u.userNotificationSetting
            WHERE ic.category = :category AND ic.isAlarmEnabled = true
            """)
    List<InterestCategory> findSubscribersWithNotificationSettings(@Param("category") Category category);

    List<InterestCategory> findByUserId(Long userId);

    boolean existsByUserAndCategory(User user, Category category);

    Optional<InterestCategory> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("UPDATE InterestCategory ic SET ic.deletedAt = CURRENT_TIMESTAMP "
            + "WHERE ic.user.id = :userId AND ic.deletedAt IS NULL")
    int softDeleteByUserId(@Param("userId") Long userId);
}
