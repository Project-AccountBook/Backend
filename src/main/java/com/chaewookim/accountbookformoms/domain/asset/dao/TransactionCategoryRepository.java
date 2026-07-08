package com.chaewookim.accountbookformoms.domain.asset.dao;

import com.chaewookim.accountbookformoms.domain.asset.entity.TransactionCategory;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransactionCategoryRepository extends JpaRepository<TransactionCategory, Long> {

    @Query("SELECT c FROM TransactionCategory c WHERE c.user.id = :userId OR c.user IS NULL")
    List<TransactionCategory> findAllByUserOrSystem(@Param("userId") Long userId);

    @Query("SELECT c.id FROM TransactionCategory c WHERE c.user IS NULL")
    List<Long> findSystemCategoryIds();

    boolean existsByUserIsNullAndNameAndType(String name, TransactionType type);

    boolean existsByUserIdAndNameAndType(Long userId, String name, TransactionType type);

    boolean existsByUserIdAndNameAndTypeAndIdNot(Long userId, String name, TransactionType type, Long id);

    @Query(value = """
            SELECT *
              FROM transaction_category
             WHERE user_id = :userId
               AND name = :name
               AND type = :type
             LIMIT 1
            """, nativeQuery = true)
    Optional<TransactionCategory> findByUserIdAndNameAndTypeIncludingDeleted(@Param("userId") Long userId,
                                                                             @Param("name") String name,
                                                                             @Param("type") String type);
}
