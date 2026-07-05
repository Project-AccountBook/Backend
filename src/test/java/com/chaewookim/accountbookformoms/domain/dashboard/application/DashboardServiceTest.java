package com.chaewookim.accountbookformoms.domain.dashboard.application;

import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionRepository;
import com.chaewookim.accountbookformoms.domain.budget.application.BudgetService;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.BudgetSummaryResponse;
import com.chaewookim.accountbookformoms.domain.dashboard.dto.response.DashboardResponse;
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

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @DisplayName("대시보드 조회 - 성공")
    void getDashboard_Success() {

        // given
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

        doReturn(categoryList).when(transactionRepository).sumCategoryExpense(eq(userId), eq(yearMonth));
        doReturn(trendList).when(transactionRepository).sumMonthlyTrends(eq(userId), any());
        given(budgetService.getMonthlyBudgetSummary(eq(userId), eq(yearMonth))).willReturn(budgetSummary);

        // when
        DashboardResponse response = dashboardService.getDashboard(userId, yearMonth);

        // then
        assertThat(response).isNotNull();
        assertThat(response.categoryExpenses()).containsEntry("식비", new BigDecimal("50000"));
        assertThat(response.trends()).hasSize(1);
        assertThat(response.trends().get(0).yearMonth()).isEqualTo("2026-06");
        assertThat(response.budgetStatus().actualExpense()).isEqualByComparingTo("50000");
        assertThat(response.summary().totalExpense()).isEqualByComparingTo("50000");
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

        doReturn(categoryList).when(transactionRepository).sumCategoryExpense(eq(userId), eq(yearMonth));
        doReturn(trendList).when(transactionRepository).sumMonthlyTrends(eq(userId), any());
        given(budgetService.getMonthlyBudgetSummary(eq(userId), eq(yearMonth))).willReturn(budgetSummary);

        DashboardResponse response = dashboardService.getDashboard(userId, yearMonth);

        assertThat(response.categoryExpenses().get("식비")).isEqualByComparingTo("80000");
        assertThat(response.trends()).hasSize(1);
        assertThat(response.trends().get(0).income()).isEqualByComparingTo("100000");
        assertThat(response.trends().get(0).expense()).isEqualByComparingTo("50000");
    }
}