package com.chaewookim.accountbookformoms.global.scheduler;

import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionCategoryRepository;
import com.chaewookim.accountbookformoms.domain.budget.application.BudgetGroupCacheService;
import com.chaewookim.accountbookformoms.domain.expense.application.ExpenseGroupCacheService;
import com.chaewookim.accountbookformoms.domain.income.application.IncomeGroupCacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CompareCacheWarmupSchedulerTest {

    @Mock
    private TransactionCategoryRepository categoryRepository;

    @Mock
    private BudgetGroupCacheService budgetGroupCache;

    @Mock
    private ExpenseGroupCacheService expenseGroupCache;

    @Mock
    private IncomeGroupCacheService incomeGroupCache;

    @InjectMocks
    private CompareCacheWarmupScheduler scheduler;

    @Test
    @DisplayName("warm-up: 6개 나이대 × (Budget AGE + Expense AGE fixed/variable + Income AGE fixed/variable) 호출")
    void warmup_calls_age_caches_for_all_decades() {

        // given
        given(categoryRepository.findSystemCategoryIds()).willReturn(List.of());

        // when
        scheduler.warmupCompareGroupCaches();

        // then - 20대~70대 6번
        verify(budgetGroupCache, times(6)).getAgeGroupSums(anyString(), any(LocalDate.class), any(LocalDate.class));
        verify(expenseGroupCache, times(6)).getAgeFixedSums(any(LocalDate.class), any(LocalDate.class), any(LocalDate.class), any(LocalDate.class));
        verify(expenseGroupCache, times(6)).getAgeVariableSums(any(LocalDate.class), any(LocalDate.class), any(LocalDate.class), any(LocalDate.class));
        verify(incomeGroupCache, times(6)).getAgeFixedSums(any(LocalDate.class), any(LocalDate.class), any(LocalDate.class), any(LocalDate.class));
        verify(incomeGroupCache, times(6)).getAgeVariableSums(any(LocalDate.class), any(LocalDate.class), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("warm-up: 시스템 카테고리 각 id 마다 Budget + Expense(fixed+variable) + Income(fixed+variable) 호출")
    void warmup_calls_category_caches_per_system_category() {

        // given - 시스템 카테고리 3개
        given(categoryRepository.findSystemCategoryIds()).willReturn(List.of(10L, 20L, 30L));

        // when
        scheduler.warmupCompareGroupCaches();

        // then - 카테고리 3 × 호출 5종
        verify(budgetGroupCache, times(3)).getCategoryAggregation(anyString(), anyLong());
        verify(expenseGroupCache, times(3)).getCategoryFixedSums(any(LocalDate.class), any(LocalDate.class), anyLong());
        verify(expenseGroupCache, times(3)).getCategoryVariableSums(any(LocalDate.class), any(LocalDate.class), anyLong());
        verify(incomeGroupCache, times(3)).getCategoryFixedSums(any(LocalDate.class), any(LocalDate.class), anyLong());
        verify(incomeGroupCache, times(3)).getCategoryVariableSums(any(LocalDate.class), any(LocalDate.class), anyLong());
    }

    @Test
    @DisplayName("warm-up: 한 카테고리에서 예외 발생해도 나머지 진행")
    void warmup_continues_on_error() {

        // given
        given(categoryRepository.findSystemCategoryIds()).willReturn(List.of(10L, 20L));
        given(budgetGroupCache.getCategoryAggregation(anyString(), eqL(10L)))
                .willThrow(new RuntimeException("intentional"));

        // when - 예외 잡혀서 throw 안 됨
        scheduler.warmupCompareGroupCaches();

        // then - 두 번째 카테고리도 처리
        verify(budgetGroupCache).getCategoryAggregation(anyString(), eqL(20L));
    }

    private static long eqL(long value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
