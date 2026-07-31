package com.chaewookim.accountbookformoms.domain.budget.dao;

import com.chaewookim.accountbookformoms.domain.budget.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    boolean existsByTransactionCategoryId(Long categoryId);

    Optional<Budget> findByUserIdAndYearMonthAndTransactionCategoryId(Long userId, String yearMonth, Long categoryId);

    @Query(value = """
            SELECT *
              FROM budget
             WHERE user_id = :userId
               AND `year_month` = :yearMonth
               AND category_id = :categoryId
             LIMIT 1
            """, nativeQuery = true)
    Optional<Budget> findByUserIdAndYearMonthAndCategoryIdIncludingDeleted(@Param("userId") Long userId,
                                                                           @Param("yearMonth") String yearMonth,
                                                                           @Param("categoryId") Long categoryId);

    @Query("""
            SELECT b
              FROM Budget b
              LEFT JOIN FETCH b.transactionCategory
             WHERE b.user.id = :userId
               AND b.yearMonth = :yearMonth
            """)
    List<Budget> findByUserIdAndYearMonth(@Param("userId") Long userId,
                                          @Param("yearMonth") String yearMonth);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Budget b
               SET b.snapshotCategoryId = :categoryId,
                   b.snapshotCategoryName = :categoryName
             WHERE b.transactionCategory.id = :categoryId
               AND b.snapshotCategoryName IS NULL
            """)
    void backfillCategorySnapshot(@Param("categoryId") Long categoryId, @Param("categoryName") String categoryName);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Budget b SET b.categoryArchived = true WHERE b.snapshotCategoryId = :categoryId")
    void markCategoryArchived(@Param("categoryId") Long categoryId);

    @Query(value = """
            SELECT b.`year_month`
              FROM budget b
             WHERE b.user_id = :userId
               AND b.`year_month` < :targetYearMonth
               AND b.deleted_at IS NULL
             GROUP BY b.`year_month`
             ORDER BY b.`year_month` DESC
             LIMIT 1
            """, nativeQuery = true)
    Optional<String> findLatestBudgetYearMonthBefore(@Param("userId") Long userId,
                                                     @Param("targetYearMonth") String targetYearMonth);

    @Query("""
            SELECT COALESCE(SUM(b.totalBudget), 0)
              FROM Budget b
             WHERE b.user.id = :userId
               AND b.yearMonth = :yearMonth
            """)
    BigDecimal sumTotalBudgetByUserIdAndYearMonth(@Param("userId") Long userId,
                                                  @Param("yearMonth") String yearMonth);

    /**
     * 특정 월에 포트폴리오 활동(고정/변동 수입·지출, 예산)이 하나라도 있는
     * 공개 사용자들의 월 총 예산 목록. 예산이 없는 사용자는 totalBudget = 0.
     * 종합 비교 페이지의 사용자 집합과 일치하도록 UNION 기반 EXISTS 로 판정.
     * 컬럼 순서: userId, username, yearMonth, totalBudget
     */
    @Query(value = """
            SELECT u.id                                                AS user_id,
                   u.username                                          AS username,
                   :yearMonth                                          AS year_month,
                   COALESCE(SUM(b.total_budget), 0)                    AS total_budget
              FROM `user` u
              JOIN user_setting s ON s.user_id = u.id AND s.is_portfolio_public = TRUE
              LEFT JOIN budget b
                     ON b.user_id = u.id
                    AND b.`year_month` = :yearMonth
                    AND b.deleted_at IS NULL
             WHERE u.deleted_at IS NULL
               AND (
                    EXISTS (SELECT 1 FROM fixed_transaction ft
                             WHERE ft.user_id = u.id
                               AND ft.type = 'INCOME'
                               AND ft.is_active = TRUE
                               AND ft.deleted_at IS NULL
                               AND ft.start_date <= :endDate
                               AND (ft.end_date IS NULL OR ft.end_date >= :startDate))
                 OR EXISTS (SELECT 1 FROM fixed_transaction ft
                             WHERE ft.user_id = u.id
                               AND ft.type = 'EXPENSE'
                               AND ft.is_active = TRUE
                               AND ft.deleted_at IS NULL
                               AND ft.start_date <= :endDate
                               AND (ft.end_date IS NULL OR ft.end_date >= :startDate))
                 OR EXISTS (SELECT 1 FROM `transaction` t
                             WHERE t.user_id = u.id
                               AND t.deleted_at IS NULL
                               AND t.transaction_date BETWEEN :startDate AND :endDate)
                 OR EXISTS (SELECT 1 FROM budget b2
                             WHERE b2.user_id = u.id
                               AND b2.`year_month` = :yearMonth
                               AND b2.deleted_at IS NULL)
               )
             GROUP BY u.id, u.username
            HAVING (:minAmount IS NULL OR COALESCE(SUM(b.total_budget), 0) >= :minAmount)
               AND (:maxAmount IS NULL OR COALESCE(SUM(b.total_budget), 0) <= :maxAmount)
             ORDER BY total_budget DESC
            """, nativeQuery = true)
    List<Object[]> findPublicMonthlyTotals(@Param("yearMonth") String yearMonth,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate,
                                           @Param("minAmount") BigDecimal minAmount,
                                           @Param("maxAmount") BigDecimal maxAmount);

    /**
     * 같은 나이대(생년월일 범위) 공개 사용자들의 월 총 예산 합계(사용자 단위).
     * 반환: [userId, totalSum]
     */
    @Query("""
            SELECT b.user.id, SUM(b.totalBudget)
              FROM Budget b
              JOIN b.user u
              JOIN u.userSetting s
             WHERE s.isPortfolioPublic = true
               AND u.id <> :excludeUserId
               AND b.yearMonth = :yearMonth
               AND u.birthDate IS NOT NULL
               AND u.birthDate BETWEEN :birthFrom AND :birthTo
             GROUP BY b.user.id
            """)
    List<Object[]> sumMonthlyTotalsByAgeRange(@Param("yearMonth") String yearMonth,
                                              @Param("birthFrom") LocalDate birthFrom,
                                              @Param("birthTo") LocalDate birthTo,
                                              @Param("excludeUserId") Long excludeUserId);

    /**
     * 지정 금액 구간 안에 드는 공개 사용자들의 월 총 예산 합계(사용자 단위).
     */
    @Query("""
            SELECT b.user.id, SUM(b.totalBudget)
              FROM Budget b
              JOIN b.user u
              JOIN u.userSetting s
             WHERE s.isPortfolioPublic = true
               AND u.id <> :excludeUserId
               AND b.yearMonth = :yearMonth
             GROUP BY b.user.id
            HAVING (:minAmount IS NULL OR SUM(b.totalBudget) >= :minAmount)
               AND (:maxAmount IS NULL OR SUM(b.totalBudget) <= :maxAmount)
            """)
    List<Object[]> sumMonthlyTotalsByAmountRange(@Param("yearMonth") String yearMonth,
                                                 @Param("minAmount") BigDecimal minAmount,
                                                 @Param("maxAmount") BigDecimal maxAmount,
                                                 @Param("excludeUserId") Long excludeUserId);

    /**
     * 같은 카테고리에 예산을 설정한 공개 사용자들의 totalBudget 평균.
     */
    @Query("""
            SELECT AVG(b.totalBudget), COUNT(b)
              FROM Budget b
              JOIN b.user u
              JOIN u.userSetting s
             WHERE s.isPortfolioPublic = true
               AND u.id <> :excludeUserId
               AND b.yearMonth = :yearMonth
               AND b.transactionCategory.id = :categoryId
            """)
    Object[] averageCategoryBudget(@Param("yearMonth") String yearMonth,
                                   @Param("categoryId") Long categoryId,
                                   @Param("excludeUserId") Long excludeUserId);

    @Query("""
            SELECT b.totalBudget
              FROM Budget b
             WHERE b.user.id = :userId
               AND b.yearMonth = :yearMonth
               AND b.transactionCategory.id = :categoryId
            """)
    BigDecimal findMyCategoryBudget(@Param("userId") Long userId,
                                    @Param("yearMonth") String yearMonth,
                                    @Param("categoryId") Long categoryId);

    /**
     * 지정 userId 집합(공개 사용자만) 의 월 총 예산 합계(사용자 단위). 위치 기반 비교용.
     * 반환: [userId, totalSum]
     */
    @Query("""
            SELECT b.user.id, SUM(b.totalBudget)
              FROM Budget b
              JOIN b.user u
              JOIN u.userSetting s
             WHERE s.isPortfolioPublic = true
               AND u.id IN :userIds
               AND b.yearMonth = :yearMonth
             GROUP BY b.user.id
            """)
    List<Object[]> sumMonthlyTotalsByUserIds(@Param("yearMonth") String yearMonth,
                                             @Param("userIds") Collection<Long> userIds);
}
