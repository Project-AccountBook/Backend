package com.chaewookim.accountbookformoms.domain.portfolio.dao;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PortfolioAggregationRepository {

    private final EntityManager em;

    public record PublicPortfolioRow(
            Long userId,
            String username,
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal totalBudget
    ) {}

    private static final String AGGREGATE_SQL = """
            SELECT
                u.id                                                                       AS user_id,
                u.username                                                                 AS username,
                COALESCE(SUM(CASE WHEN agg.src IN ('IF','IV') THEN agg.amt END), 0)        AS total_income,
                COALESCE(SUM(CASE WHEN agg.src IN ('EF','EV') THEN agg.amt END), 0)        AS total_expense,
                COALESCE(SUM(CASE WHEN agg.src = 'B'           THEN agg.amt END), 0)        AS total_budget
            FROM (
                SELECT user_id, amount AS amt, 'IF' AS src
                  FROM fixed_transaction
                 WHERE type = 'INCOME' AND is_active = TRUE AND deleted_at IS NULL
                   AND start_date <= :endDate
                   AND (end_date IS NULL OR end_date >= :startDate)
                UNION ALL
                SELECT user_id, amount, 'IV'
                  FROM `transaction`
                 WHERE type = 'INCOME' AND deleted_at IS NULL
                   AND transaction_date BETWEEN :startDate AND :endDate
                UNION ALL
                SELECT user_id, amount, 'EF'
                  FROM fixed_transaction
                 WHERE type = 'EXPENSE' AND is_active = TRUE AND deleted_at IS NULL
                   AND start_date <= :endDate
                   AND (end_date IS NULL OR end_date >= :startDate)
                UNION ALL
                SELECT user_id, amount, 'EV'
                  FROM `transaction`
                 WHERE type = 'EXPENSE' AND deleted_at IS NULL
                   AND transaction_date BETWEEN :startDate AND :endDate
                UNION ALL
                SELECT user_id, total_budget AS amt, 'B'
                  FROM budget
                 WHERE deleted_at IS NULL
                   AND `year_month` = :yearMonth
            ) agg
            JOIN `user`        u ON u.id = agg.user_id AND u.deleted_at IS NULL
            JOIN user_setting  s ON s.user_id = u.id AND s.is_portfolio_public = TRUE
            GROUP BY u.id, u.username
            HAVING (:minIncome  IS NULL OR COALESCE(SUM(CASE WHEN agg.src IN ('IF','IV') THEN agg.amt END), 0) >= :minIncome)
               AND (:maxIncome  IS NULL OR COALESCE(SUM(CASE WHEN agg.src IN ('IF','IV') THEN agg.amt END), 0) <= :maxIncome)
               AND (:minExpense IS NULL OR COALESCE(SUM(CASE WHEN agg.src IN ('EF','EV') THEN agg.amt END), 0) >= :minExpense)
               AND (:maxExpense IS NULL OR COALESCE(SUM(CASE WHEN agg.src IN ('EF','EV') THEN agg.amt END), 0) <= :maxExpense)
               AND (:minBudget  IS NULL OR COALESCE(SUM(CASE WHEN agg.src = 'B'           THEN agg.amt END), 0) >= :minBudget)
               AND (:maxBudget  IS NULL OR COALESCE(SUM(CASE WHEN agg.src = 'B'           THEN agg.amt END), 0) <= :maxBudget)
            ORDER BY
                (COALESCE(SUM(CASE WHEN agg.src IN ('IF','IV') THEN agg.amt END), 0)
               - COALESCE(SUM(CASE WHEN agg.src IN ('EF','EV') THEN agg.amt END), 0)) DESC
            """;

    @SuppressWarnings("unchecked")
    public List<PublicPortfolioRow> findPublicPortfolios(
            LocalDate startDate,
            LocalDate endDate,
            String yearMonth,
            BigDecimal minIncome, BigDecimal maxIncome,
            BigDecimal minExpense, BigDecimal maxExpense,
            BigDecimal minBudget, BigDecimal maxBudget
    ) {
        List<Object[]> rows = em.createNativeQuery(AGGREGATE_SQL)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .setParameter("yearMonth", yearMonth)
                .setParameter("minIncome", minIncome)
                .setParameter("maxIncome", maxIncome)
                .setParameter("minExpense", minExpense)
                .setParameter("maxExpense", maxExpense)
                .setParameter("minBudget", minBudget)
                .setParameter("maxBudget", maxBudget)
                .getResultList();

        return rows.stream()
                .map(r -> new PublicPortfolioRow(
                        ((Number) r[0]).longValue(),
                        (String) r[1],
                        toBigDecimal(r[2]),
                        toBigDecimal(r[3]),
                        toBigDecimal(r[4])))
                .toList();
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }
}
