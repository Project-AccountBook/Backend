package com.chaewookim.accountbookformoms.domain.budget.application;

import com.chaewookim.accountbookformoms.domain.budget.dao.BudgetRepository;
import com.chaewookim.accountbookformoms.global.config.RedisConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 그룹 평균 비교를 위한 캐싱 wrapper.
 * <p>userId 를 키에서 제외해 같은 demographic 사용자들이 공유. self exclusion 은 호출자(CompareService) 가 in-memory 로 처리.</p>
 * <p>repo 쿼리의 excludeUserId 자리에 {@link #NO_EXCLUDE} 를 넘겨 "모든 공개 사용자 포함" 결과를 캐시.</p>
 */
@Service
@RequiredArgsConstructor
public class BudgetGroupCacheService {

    private static final Long NO_EXCLUDE = -1L;

    private final BudgetRepository budgetRepository;

    public record CategoryAggregation(BigDecimal totalSum, long count) {}

    @Cacheable(
            cacheNames = RedisConfig.CACHE_GROUP_BUDGET_AGE,
            key = "T(java.util.Objects).hash(#yearMonth, #birthFrom.toString(), #birthTo.toString())")
    public Map<Long, BigDecimal> getAgeGroupSums(String yearMonth, LocalDate birthFrom, LocalDate birthTo) {
        return toUserSumMap(budgetRepository.sumMonthlyTotalsByAgeRange(yearMonth, birthFrom, birthTo, NO_EXCLUDE));
    }

    @Cacheable(
            cacheNames = RedisConfig.CACHE_GROUP_BUDGET_AMOUNT,
            key = "T(java.util.Objects).hash(#yearMonth, #minAmount, #maxAmount)")
    public Map<Long, BigDecimal> getAmountGroupSums(String yearMonth, BigDecimal minAmount, BigDecimal maxAmount) {
        return toUserSumMap(budgetRepository.sumMonthlyTotalsByAmountRange(yearMonth, minAmount, maxAmount, NO_EXCLUDE));
    }

    @Cacheable(
            cacheNames = RedisConfig.CACHE_GROUP_BUDGET_CATEGORY,
            key = "T(java.util.Objects).hash(#yearMonth, #categoryId)")
    public CategoryAggregation getCategoryAggregation(String yearMonth, Long categoryId) {
        Object[] row = budgetRepository.averageCategoryBudget(yearMonth, categoryId, NO_EXCLUDE);
        if (row == null || row.length < 2 || row[1] == null) {
            return new CategoryAggregation(BigDecimal.ZERO, 0L);
        }
        long count = ((Number) row[1]).longValue();
        if (count <= 0 || row[0] == null) {
            return new CategoryAggregation(BigDecimal.ZERO, 0L);
        }
        BigDecimal avg = toBigDecimal(row[0]);
        BigDecimal totalSum = avg.multiply(BigDecimal.valueOf(count));
        return new CategoryAggregation(totalSum, count);
    }

    private static Map<Long, BigDecimal> toUserSumMap(List<Object[]> rows) {
        Map<Long, BigDecimal> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put(((Number) row[0]).longValue(), toBigDecimal(row[1]));
        }
        return map;
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }
}
