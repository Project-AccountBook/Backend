package com.chaewookim.accountbookformoms.domain.budget.dao;

import com.chaewookim.accountbookformoms.domain.budget.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    @Query("""
            SELECT b
              FROM Budget b
              LEFT JOIN FETCH b.transactionCategory
             WHERE b.user.id = :userId
               AND b.yearMonth = :yearMonth
            """)
    List<Budget> findByUserIdAndYearMonth(@Param("userId") Long userId,
                                          @Param("yearMonth") String yearMonth);

    @Query("""
            SELECT COALESCE(SUM(b.totalBudget), 0)
              FROM Budget b
             WHERE b.user.id = :userId
               AND b.yearMonth = :yearMonth
            """)
    BigDecimal sumTotalBudgetByUserIdAndYearMonth(@Param("userId") Long userId,
                                                  @Param("yearMonth") String yearMonth);

    /**
     * 공개 설정한 사용자들의 (user, year_month)별 총 예산 목록.
     * (필요한 필드만 Object[] 로 반환 — 컬럼 순서: userId, username, yearMonth, totalSum)
     */
    @Query("""
            SELECT b.user.id, b.user.username, b.yearMonth, SUM(b.totalBudget)
              FROM Budget b
              JOIN b.user u
              JOIN u.userSetting s
             WHERE s.isPortfolioPublic = true
               AND (:yearMonthFrom IS NULL OR b.yearMonth >= :yearMonthFrom)
               AND (:yearMonthTo   IS NULL OR b.yearMonth <= :yearMonthTo)
             GROUP BY b.user.id, b.user.username, b.yearMonth
            HAVING (:minAmount IS NULL OR SUM(b.totalBudget) >= :minAmount)
               AND (:maxAmount IS NULL OR SUM(b.totalBudget) <= :maxAmount)
             ORDER BY b.yearMonth DESC, SUM(b.totalBudget) DESC
            """)
    List<Object[]> findPublicMonthlyTotals(@Param("yearMonthFrom") String yearMonthFrom,
                                           @Param("yearMonthTo") String yearMonthTo,
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
}
