package com.chaewookim.accountbookformoms.global.scheduler;

import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionCategoryRepository;
import com.chaewookim.accountbookformoms.domain.budget.application.BudgetGroupCacheService;
import com.chaewookim.accountbookformoms.domain.expense.application.ExpenseGroupCacheService;
import com.chaewookim.accountbookformoms.domain.income.application.IncomeGroupCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Compare 그룹 캐시 warm-up.
 * <p>현재 월 기준으로 (1) 시스템 카테고리 모든 id × Budget/Expense(fixed+variable)/Income(fixed+variable),
 * (2) 6개 나이대 (20-70대) × 3 도메인 의 그룹 평균을 미리 캐시에 적재.</p>
 * <p>AMOUNT axis 는 표준 버킷 정의가 없어 미포함, LOCATION 은 사용자별 반경 결과라 cross-user hit 0% 라 미포함.</p>
 * <p>다중 인스턴스 환경 중복 실행은 ShedLock 으로 차단.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.cache.warmup.compare.enabled", havingValue = "true", matchIfMissing = true)
public class CompareCacheWarmupScheduler {

    private static final List<Integer> WARMUP_DECADES = List.of(20, 30, 40, 50, 60, 70);

    private final TransactionCategoryRepository categoryRepository;
    private final BudgetGroupCacheService budgetGroupCache;
    private final ExpenseGroupCacheService expenseGroupCache;
    private final IncomeGroupCacheService incomeGroupCache;

    @Scheduled(cron = "0 */30 * * * *")
    @SchedulerLock(name = "CompareCacheWarmupScheduler_warmupCompareGroupCaches",
            lockAtMostFor = "PT20M", lockAtLeastFor = "PT1M")
    public void warmupCompareGroupCaches() {

        YearMonth ym = YearMonth.now();
        String yearMonth = ym.toString();
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();
        int currentYear = LocalDate.now().getYear();
        long start = System.currentTimeMillis();

        log.info("Compare cache warm-up 시작: yearMonth={}", yearMonth);

        warmupAge(yearMonth, startDate, endDate, currentYear);
        warmupCategory(yearMonth, startDate, endDate);

        log.info("Compare cache warm-up 완료: {}ms", System.currentTimeMillis() - start);
    }

    private void warmupAge(String yearMonth, LocalDate startDate, LocalDate endDate, int currentYear) {
        for (int decade : WARMUP_DECADES) {
            LocalDate birthFrom = LocalDate.of(currentYear - (decade + 9), 1, 1);
            LocalDate birthTo = LocalDate.of(currentYear - decade, 12, 31);
            try {
                budgetGroupCache.getAgeGroupSums(yearMonth, birthFrom, birthTo);
                expenseGroupCache.getAgeFixedSums(startDate, endDate, birthFrom, birthTo);
                expenseGroupCache.getAgeVariableSums(startDate, endDate, birthFrom, birthTo);
                incomeGroupCache.getAgeFixedSums(startDate, endDate, birthFrom, birthTo);
                incomeGroupCache.getAgeVariableSums(startDate, endDate, birthFrom, birthTo);
            } catch (Exception e) {
                log.warn("AGE warm-up 실패: decade={}, error={}", decade, e.getMessage());
            }
        }
    }

    private void warmupCategory(String yearMonth, LocalDate startDate, LocalDate endDate) {
        List<Long> categoryIds = categoryRepository.findSystemCategoryIds();
        for (Long categoryId : categoryIds) {
            try {
                budgetGroupCache.getCategoryAggregation(yearMonth, categoryId);
                expenseGroupCache.getCategoryFixedSums(startDate, endDate, categoryId);
                expenseGroupCache.getCategoryVariableSums(startDate, endDate, categoryId);
                incomeGroupCache.getCategoryFixedSums(startDate, endDate, categoryId);
                incomeGroupCache.getCategoryVariableSums(startDate, endDate, categoryId);
            } catch (Exception e) {
                log.warn("CATEGORY warm-up 실패: categoryId={}, error={}", categoryId, e.getMessage());
            }
        }
        log.info("CATEGORY warm-up: {} 카테고리 처리", categoryIds.size());
    }
}
