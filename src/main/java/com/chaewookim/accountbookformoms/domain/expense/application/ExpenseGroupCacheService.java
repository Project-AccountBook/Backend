package com.chaewookim.accountbookformoms.domain.expense.application;

import com.chaewookim.accountbookformoms.domain.asset.dao.FixedTransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
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
 * 지출 그룹 평균 캐싱 wrapper. fixed/variable 각각 별도 캐시.
 */
@Service
@RequiredArgsConstructor
public class ExpenseGroupCacheService {

    private static final Long NO_EXCLUDE = -1L;
    private static final TransactionType TYPE = TransactionType.EXPENSE;

    private final FixedTransactionRepository fixedTransactionRepository;
    private final TransactionRepository transactionRepository;

    @Cacheable(
            cacheNames = RedisConfig.CACHE_GROUP_EXPENSE_AGE_FIXED,
            key = "T(java.util.Objects).hash(#startDate.toString(), #endDate.toString(), #birthFrom.toString(), #birthTo.toString())")
    public Map<Long, BigDecimal> getAgeFixedSums(LocalDate startDate, LocalDate endDate, LocalDate birthFrom, LocalDate birthTo) {
        return toUserSumMap(fixedTransactionRepository.sumPublicByAgeRange(TYPE, startDate, endDate, birthFrom, birthTo, NO_EXCLUDE));
    }

    @Cacheable(
            cacheNames = RedisConfig.CACHE_GROUP_EXPENSE_AGE_VARIABLE,
            key = "T(java.util.Objects).hash(#startDate.toString(), #endDate.toString(), #birthFrom.toString(), #birthTo.toString())")
    public Map<Long, BigDecimal> getAgeVariableSums(LocalDate startDate, LocalDate endDate, LocalDate birthFrom, LocalDate birthTo) {
        return toUserSumMap(transactionRepository.sumPublicByAgeRange(TYPE, startDate, endDate, birthFrom, birthTo, NO_EXCLUDE));
    }

    @Cacheable(
            cacheNames = RedisConfig.CACHE_GROUP_EXPENSE_AMOUNT_FIXED,
            key = "T(java.util.Objects).hash(#startDate.toString(), #endDate.toString(), #minAmount, #maxAmount)")
    public Map<Long, BigDecimal> getAmountFixedSums(LocalDate startDate, LocalDate endDate, BigDecimal minAmount, BigDecimal maxAmount) {
        return toUserSumMap(fixedTransactionRepository.sumPublicByAmountRange(TYPE, startDate, endDate, minAmount, maxAmount, NO_EXCLUDE));
    }

    @Cacheable(
            cacheNames = RedisConfig.CACHE_GROUP_EXPENSE_AMOUNT_VARIABLE,
            key = "T(java.util.Objects).hash(#startDate.toString(), #endDate.toString(), #minAmount, #maxAmount)")
    public Map<Long, BigDecimal> getAmountVariableSums(LocalDate startDate, LocalDate endDate, BigDecimal minAmount, BigDecimal maxAmount) {
        return toUserSumMap(transactionRepository.sumPublicByAmountRange(TYPE, startDate, endDate, minAmount, maxAmount, NO_EXCLUDE));
    }

    @Cacheable(
            cacheNames = RedisConfig.CACHE_GROUP_EXPENSE_CATEGORY_FIXED,
            key = "T(java.util.Objects).hash(#startDate.toString(), #endDate.toString(), #categoryId)")
    public Map<Long, BigDecimal> getCategoryFixedSums(LocalDate startDate, LocalDate endDate, Long categoryId) {
        return toUserSumMap(fixedTransactionRepository.sumPublicCategoryByUser(TYPE, categoryId, startDate, endDate, NO_EXCLUDE));
    }

    @Cacheable(
            cacheNames = RedisConfig.CACHE_GROUP_EXPENSE_CATEGORY_VARIABLE,
            key = "T(java.util.Objects).hash(#startDate.toString(), #endDate.toString(), #categoryId)")
    public Map<Long, BigDecimal> getCategoryVariableSums(LocalDate startDate, LocalDate endDate, Long categoryId) {
        return toUserSumMap(transactionRepository.sumPublicCategoryByUser(TYPE, categoryId, startDate, endDate, NO_EXCLUDE));
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
