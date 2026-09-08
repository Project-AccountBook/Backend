package com.chaewookim.accountbookformoms.domain.asset.dao;

import com.chaewookim.accountbookformoms.domain.asset.entity.FixedTransaction;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface FixedTransactionRepository extends JpaRepository<FixedTransaction, Long> {

    List<FixedTransaction> findAllByUserId(Long userId);

    @Query("SELECT f FROM FixedTransaction f WHERE f.targetAccount.id = :accountId")
    List<FixedTransaction> findAllByTargetAccountId(@Param("accountId") Long accountId);

    @Modifying
    @Query("UPDATE FixedTransaction f SET f.deletedAt = CURRENT_TIMESTAMP "
            + "WHERE f.account.id = :accountId AND f.deletedAt IS NULL")
    int softDeleteByAccountId(@Param("accountId") Long accountId);

    @Modifying
    @Query("UPDATE FixedTransaction f SET f.deletedAt = CURRENT_TIMESTAMP "
            + "WHERE f.transactionCategory.id = :categoryId AND f.deletedAt IS NULL")
    int softDeleteByTransactionCategoryId(@Param("categoryId") Long categoryId);

    List<FixedTransaction> findAllByIsActiveTrue();

    List<FixedTransaction> findAllByIsActiveTrueAndNextExecutionDateLessThanEqual(LocalDate today);

    /**
     * 사용자의 해당 월에 적용되는 고정(FixedTransaction) 거래 카테고리별 합계.
     * 반환 컬럼: [categoryId, categoryName, sumAmount]
     */
    @Query("""
            SELECT f.transactionCategory.id, f.transactionCategory.name, SUM(f.amount)
              FROM FixedTransaction f
             WHERE f.user.id = :userId
               AND f.type = :type
               AND f.isActive = true
               AND f.startDate <= :endDate
               AND (f.endDate IS NULL OR f.endDate >= :startDate)
             GROUP BY f.transactionCategory.id, f.transactionCategory.name
            """)
    List<Object[]> sumByUserCategory(@Param("userId") Long userId,
                                     @Param("type") TransactionType type,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT COALESCE(SUM(f.amount), 0)
              FROM FixedTransaction f
             WHERE f.user.id = :userId
               AND f.type = :type
               AND f.isActive = true
               AND f.startDate <= :endDate
               AND (f.endDate IS NULL OR f.endDate >= :startDate)
            """)
    BigDecimal sumByUserAndType(@Param("userId") Long userId,
                                @Param("type") TransactionType type,
                                @Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT COALESCE(SUM(f.amount), 0)
              FROM FixedTransaction f
             WHERE f.user.id = :userId
               AND f.type = :type
               AND f.transactionCategory.id = :categoryId
               AND f.isActive = true
               AND f.startDate <= :endDate
               AND (f.endDate IS NULL OR f.endDate >= :startDate)
            """)
    BigDecimal sumByUserAndTypeAndCategory(@Param("userId") Long userId,
                                           @Param("type") TransactionType type,
                                           @Param("categoryId") Long categoryId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    /**
     * 공개 설정 사용자들의 (user)별 월 합계.
     * 반환 컬럼: [userId, username, sumAmount]
     */
    @Query("""
            SELECT f.user.id, f.user.username, SUM(f.amount)
              FROM FixedTransaction f
              JOIN f.user u
              JOIN u.userSetting s
             WHERE s.isPortfolioPublic = true
               AND f.type = :type
               AND f.isActive = true
               AND f.startDate <= :endDate
               AND (f.endDate IS NULL OR f.endDate >= :startDate)
             GROUP BY f.user.id, f.user.username
            """)
    List<Object[]> sumPublicMonthly(@Param("type") TransactionType type,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT f.user.id, SUM(f.amount)
              FROM FixedTransaction f
              JOIN f.user u
              JOIN u.userSetting s
             WHERE s.isPortfolioPublic = true
               AND u.id <> :excludeUserId
               AND f.type = :type
               AND f.isActive = true
               AND f.startDate <= :endDate
               AND (f.endDate IS NULL OR f.endDate >= :startDate)
               AND u.birthDate IS NOT NULL
               AND u.birthDate BETWEEN :birthFrom AND :birthTo
             GROUP BY f.user.id
            """)
    List<Object[]> sumPublicByAgeRange(@Param("type") TransactionType type,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate,
                                       @Param("birthFrom") LocalDate birthFrom,
                                       @Param("birthTo") LocalDate birthTo,
                                       @Param("excludeUserId") Long excludeUserId);

    @Query("""
            SELECT f.user.id, SUM(f.amount)
              FROM FixedTransaction f
              JOIN f.user u
              JOIN u.userSetting s
             WHERE s.isPortfolioPublic = true
               AND u.id <> :excludeUserId
               AND f.type = :type
               AND f.isActive = true
               AND f.startDate <= :endDate
               AND (f.endDate IS NULL OR f.endDate >= :startDate)
             GROUP BY f.user.id
            HAVING (:minAmount IS NULL OR SUM(f.amount) >= :minAmount)
               AND (:maxAmount IS NULL OR SUM(f.amount) <= :maxAmount)
            """)
    List<Object[]> sumPublicByAmountRange(@Param("type") TransactionType type,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate,
                                          @Param("minAmount") BigDecimal minAmount,
                                          @Param("maxAmount") BigDecimal maxAmount,
                                          @Param("excludeUserId") Long excludeUserId);

    @Query("""
            SELECT f.user.id, SUM(f.amount)
              FROM FixedTransaction f
              JOIN f.user u
              JOIN u.userSetting s
             WHERE s.isPortfolioPublic = true
               AND u.id <> :excludeUserId
               AND f.type = :type
               AND f.transactionCategory.id = :categoryId
               AND f.isActive = true
               AND f.startDate <= :endDate
               AND (f.endDate IS NULL OR f.endDate >= :startDate)
             GROUP BY f.user.id
            """)
    List<Object[]> sumPublicCategoryByUser(@Param("type") TransactionType type,
                                           @Param("categoryId") Long categoryId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate,
                                           @Param("excludeUserId") Long excludeUserId);

    /**
     * 지정 userId 집합(공개 사용자만) 의 월 고정 거래 합계(사용자 단위). 위치 기반 비교용.
     */
    @Query("""
            SELECT f.user.id, SUM(f.amount)
              FROM FixedTransaction f
              JOIN f.user u
              JOIN u.userSetting s
             WHERE s.isPortfolioPublic = true
               AND u.id IN :userIds
               AND f.type = :type
               AND f.isActive = true
               AND f.startDate <= :endDate
               AND (f.endDate IS NULL OR f.endDate >= :startDate)
             GROUP BY f.user.id
            """)
    List<Object[]> sumPublicByUserIds(@Param("type") TransactionType type,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate,
                                      @Param("userIds") Collection<Long> userIds);
}
