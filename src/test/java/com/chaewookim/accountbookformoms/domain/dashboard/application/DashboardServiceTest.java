package com.chaewookim.accountbookformoms.domain.dashboard.application;

import com.chaewookim.accountbookformoms.domain.asset.application.MonthlyAllocationService;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.dto.response.AllocationBucketResponse;
import com.chaewookim.accountbookformoms.domain.asset.dto.response.MonthlyAllocationResponse;
import com.chaewookim.accountbookformoms.domain.budget.application.BudgetService;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.BudgetSummaryResponse;
import com.chaewookim.accountbookformoms.domain.dashboard.dto.response.DashboardResponse;
import com.chaewookim.accountbookformoms.domain.portfolio.application.PortfolioService;
import com.chaewookim.accountbookformoms.domain.portfolio.dto.response.MyPortfolioResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private BudgetService budgetService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private MonthlyAllocationService monthlyAllocationService;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @DisplayName("대시보드 조회 - 성공")
    void getDashboard_Success() {

        Long userId = 1L;
        String yearMonth = "2026-06";
        List<Object[]> categoryList = Collections.singletonList(new Object[]{"식비", new BigDecimal("50000")});
        List<Object[]> trendList = Collections.singletonList(new Object[]{"2026-06", new BigDecimal("100000"), new BigDecimal("50000")});

        BudgetSummaryResponse budgetSummary = new BudgetSummaryResponse(
                yearMonth,
                new BigDecimal("100000"),
                new BigDecimal("50000"),
                new BigDecimal("50000")
        );

        MonthlyAllocationResponse allocation = new MonthlyAllocationResponse(
                new AllocationBucketResponse(
                        new BigDecimal("200000"),
                        new BigDecimal("20.0"),
                        new BigDecimal("200000"),
                        BigDecimal.ZERO
                ),
                new AllocationBucketResponse(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                )
        );

        doReturn(categoryList).when(transactionRepository).sumCategoryExpense(eq(userId), eq(yearMonth));
        doReturn(trendList).when(transactionRepository).sumMonthlyTrends(eq(userId), any());
        given(budgetService.getMonthlyBudgetSummary(eq(userId), eq(yearMonth))).willReturn(budgetSummary);
        given(portfolioService.getMyPortfolio(userId, yearMonth)).willReturn(
                new MyPortfolioResponse(
                        yearMonth,
                        new BigDecimal("1000000"),
                        new BigDecimal("500000"),
                        new BigDecimal("100000"),
                        new BigDecimal("500000"),
                        null,
                        null,
                        null
                )
        );
        given(monthlyAllocationService.computeMonthlyAllocation(userId, yearMonth, new BigDecimal("1000000")))
                .willReturn(allocation);
        given(monthlyAllocationService.buildGoalProgress(userId)).willReturn(List.of());
        given(monthlyAllocationService.computeTotalAsset(userId)).willReturn(new BigDecimal("1500000"));

        DashboardResponse response = dashboardService.getDashboard(userId, yearMonth);

        assertThat(response).isNotNull();
        assertThat(response.categoryExpenses()).containsEntry("식비", new BigDecimal("50000"));
        assertThat(response.trends()).hasSize(1);
        assertThat(response.budgetStatus().actualExpense()).isEqualByComparingTo("50000");
        assertThat(response.allocation().savings().rate()).isEqualByComparingTo("20.0");
        assertThat(response.totalAsset()).isEqualByComparingTo("1500000");
    }

    @Test
    @DisplayName("대시보드 조회 - 동일 카테고리명 합산 및 숫자 타입 변환")
    void getDashboard_mergeDuplicateCategories() {
        Long userId = 1L;
        String yearMonth = "2026-06";
        List<Object[]> categoryList = List.of(
                new Object[]{"식비", 50000L},
                new Object[]{"식비", Double.valueOf(30000.0)}
        );
        List<Object[]> trendList = Collections.singletonList(
                new Object[]{"2026-06", 100000L, Double.valueOf(50000.0)}
        );

        BudgetSummaryResponse budgetSummary = new BudgetSummaryResponse(
                yearMonth,
                new BigDecimal("100000"),
                new BigDecimal("50000"),
                new BigDecimal("50000")
        );

        MonthlyAllocationResponse allocation = new MonthlyAllocationResponse(
                new AllocationBucketResponse(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
                new AllocationBucketResponse(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        );

        doReturn(categoryList).when(transactionRepository).sumCategoryExpense(eq(userId), eq(yearMonth));
        doReturn(trendList).when(transactionRepository).sumMonthlyTrends(eq(userId), any());
        given(budgetService.getMonthlyBudgetSummary(eq(userId), eq(yearMonth))).willReturn(budgetSummary);
        given(portfolioService.getMyPortfolio(userId, yearMonth)).willReturn(
                new MyPortfolioResponse(yearMonth, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null)
        );
        given(monthlyAllocationService.computeMonthlyAllocation(eq(userId), eq(yearMonth), any())).willReturn(allocation);
        given(monthlyAllocationService.buildGoalProgress(userId)).willReturn(List.of());
        given(monthlyAllocationService.computeTotalAsset(userId)).willReturn(BigDecimal.ZERO);

        DashboardResponse response = dashboardService.getDashboard(userId, yearMonth);

        assertThat(response.categoryExpenses().get("식비")).isEqualByComparingTo("80000");
        assertThat(response.trends()).hasSize(1);
        assertThat(response.trends().get(0).income()).isEqualByComparingTo("100000");
    }
}
