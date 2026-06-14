package com.chaewookim.accountbookformoms.domain.asset.dao;

import com.chaewookim.accountbookformoms.domain.asset.entity.Transaction;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("""
            SELECT t.transactionCategory.name, SUM(ABS(t.amount))
              FROM Transaction t
             WHERE t.user.id = :userId
               AND t.type = 'EXPENSE'
               AND FUNCTION('DATE_FORMAT', t.transactionDate, '%Y-%m') = :yearMonth
             GROUP BY t.transactionCategory.name
            """)
    List<Object[]> sumCategoryExpense(@Param("userId") Long userId, @Param("yearMonth") String yearMonth);

    @Query("""
            SELECT FUNCTION('DATE_FORMAT', t.transactionDate, '%Y-%m') as ym,
                   SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END) as totalIncome,
                   SUM(CASE WHEN t.type = 'EXPENSE' THEN ABS(t.amount) ELSE 0 END) as totalExpense
              FROM Transaction t
             WHERE t.user.id = :userId
               AND t.transactionDate >= :sixMonthsAgo
             GROUP BY FUNCTION('DATE_FORMAT', t.transactionDate, '%Y-%m')
             ORDER BY ym ASC
            """)
    List<Object[]> sumMonthlyTrends(@Param("userId") Long userId, @Param("sixMonthsAgo") LocalDate sixMonthsAgo);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
              FROM Transaction t
             WHERE t.user.id = :userId
               AND t.type = TransactionType.EXPENSE
               AND t.transactionCategory.id = :categoryId
               AND t.transactionDate BETWEEN :startDate AND :endDate
            """)
    BigDecimal sumAmountByUserIdAndCategoryId(@Param("userId") Long userId,
                                              @Param("categoryId") Long categoryId,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    @Query("""
    SELECT t.transactionCategory.id, SUM(ABS(t.amount))
    FROM Transaction t
    WHERE t.user.id = :userId
      AND FUNCTION('DATE_FORMAT', t.transactionDate, '%Y-%m') = :yearMonth
    GROUP BY t.transactionCategory.id
    """)
    List<Object[]> sumAmountByUserIdGroupByCategoryId(@Param("userId") Long userId,
                                                      @Param("yearMonth") String yearMonth);

    /**
     * 사용자의 월별 변동(Transaction) 거래 카테고리별 합계.
     * 반환 컬럼: [categoryId, categoryName, sumAmount]
     */
    @Query("""
            SELECT t.transactionCategory.id, t.transactionCategory.name, SUM(t.amount)
              FROM Transaction t
             WHERE t.user.id = :userId
               AND t.type = :type
               AND t.transactionDate BETWEEN :startDate AND :endDate
             GROUP BY t.transactionCategory.id, t.transactionCategory.name
            """)
    List<Object[]> sumByUserCategory(@Param("userId") Long userId,
                                     @Param("type") TransactionType type,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
              FROM Transaction t
             WHERE t.user.id = :userId
               AND t.type = :type
               AND t.transactionDate BETWEEN :startDate AND :endDate
            """)
    BigDecimal sumByUserAndType(@Param("userId") Long userId,
                                @Param("type") TransactionType type,
                                @Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
              FROM Transaction t
             WHERE t.user.id = :userId
               AND t.type = :type
               AND t.transactionCategory.id = :categoryId
               AND t.transactionDate BETWEEN :startDate AND :endDate
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
            SELECT t.user.id, t.user.username, SUM(t.amount)
              FROM Transaction t
              JOIN t.user u
              JOIN u.userSetting s
             WHERE s.isPortfolioPublic = true
               AND t.type = :type
               AND t.transactionDate BETWEEN :startDate AND :endDate
             GROUP BY t.user.id, t.user.username
            """)
    List<Object[]> sumPublicMonthly(@Param("type") TransactionType type,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);

    /**
     * 같은 나이대 공개 사용자들의 월 합계.
     * 반환 컬럼: [userId, sumAmount]
     */
    @Query("""
            SELECT t.user.id, SUM(t.amount)
              FROM Transaction t
              JOIN t.user u
              JOIN u.userSetting s
             WHERE s.isPortfolioPublic = true
               AND u.id <> :excludeUserId
               AND t.type = :type
               AND t.transactionDate BETWEEN :startDate AND :endDate
               AND u.birthDate IS NOT NULL
               AND u.birthDate BETWEEN :birthFrom AND :birthTo
             GROUP BY t.user.id
            """)
    List<Object[]> sumPublicByAgeRange(@Param("type") TransactionType type,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate,
                                       @Param("birthFrom") LocalDate birthFrom,
                                       @Param("birthTo") LocalDate birthTo,
                                       @Param("excludeUserId") Long excludeUserId);

    /**
     * 지정 금액 구간 안에 드는 공개 사용자들의 월 합계.
     */
    @Query("""
            SELECT t.user.id, SUM(t.amount)
              FROM Transaction t
              JOIN t.user u
              JOIN u.userSetting s
             WHERE s.isPortfolioPublic = true
               AND u.id <> :excludeUserId
               AND t.type = :type
               AND t.transactionDate BETWEEN :startDate AND :endDate
             GROUP BY t.user.id
            HAVING (:minAmount IS NULL OR SUM(t.amount) >= :minAmount)
               AND (:maxAmount IS NULL OR SUM(t.amount) <= :maxAmount)
            """)
    List<Object[]> sumPublicByAmountRange(@Param("type") TransactionType type,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate,
                                          @Param("minAmount") BigDecimal minAmount,
                                          @Param("maxAmount") BigDecimal maxAmount,
                                          @Param("excludeUserId") Long excludeUserId);

    /**
     * 공개 사용자들의 특정 카테고리 월 합계(사용자 단위).
     * 반환: [userId, sumAmount]
     */
    @Query("""
            SELECT t.user.id, SUM(t.amount)
              FROM Transaction t
              JOIN t.user u
              JOIN u.userSetting s
             WHERE s.isPortfolioPublic = true
               AND u.id <> :excludeUserId
               AND t.type = :type
               AND t.transactionCategory.id = :categoryId
               AND t.transactionDate BETWEEN :startDate AND :endDate
             GROUP BY t.user.id
            """)
    List<Object[]> sumPublicCategoryByUser(@Param("type") TransactionType type,
                                           @Param("categoryId") Long categoryId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate,
                                           @Param("excludeUserId") Long excludeUserId);
           
    Page<Transaction> findAllByUserIdAndAccountIdAndTransactionDateBetween(Long userId, Long accountId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * 지정 userId 집합(공개 사용자만) 의 월 합계(사용자 단위). 위치 기반 비교용.
     */
    @Query("""
            SELECT t.user.id, SUM(t.amount)
              FROM Transaction t
              JOIN t.user u
              JOIN u.userSetting s
             WHERE s.isPortfolioPublic = true
               AND u.id IN :userIds
               AND t.type = :type
               AND t.transactionDate BETWEEN :startDate AND :endDate
             GROUP BY t.user.id
            """)
    List<Object[]> sumPublicByUserIds(@Param("type") TransactionType type,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate,
                                      @Param("userIds") Collection<Long> userIds);
}