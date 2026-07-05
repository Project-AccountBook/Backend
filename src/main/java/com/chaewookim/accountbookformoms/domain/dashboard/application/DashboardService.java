package com.chaewookim.accountbookformoms.domain.dashboard.application;

import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionRepository;
import com.chaewookim.accountbookformoms.domain.budget.application.BudgetService;
import com.chaewookim.accountbookformoms.domain.dashboard.dto.response.BudgetStatusResponse;
import com.chaewookim.accountbookformoms.domain.dashboard.dto.response.DashboardResponse;
import com.chaewookim.accountbookformoms.domain.dashboard.dto.response.MonthlyTrendResponse;
import com.chaewookim.accountbookformoms.domain.dashboard.dto.response.SummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final BudgetService budgetService;
    private final TransactionRepository transactionRepository;

    public DashboardResponse getDashboard(Long userId, String yearMonth) {

        // 카테고리별 지출 통계
        Map<String, BigDecimal> categoryExpenses = transactionRepository.sumCategoryExpense(userId, yearMonth)
                .stream()
                .collect(Collectors.toMap(
                        row -> toString(row[0]),
                        row -> toBigDecimal(row[1]),
                        BigDecimal::add
                ));

        // 6개월 추이
        List<MonthlyTrendResponse> trends = transactionRepository.sumMonthlyTrends(userId, LocalDate.now().minusMonths(6))
                .stream()
                .map(row -> new MonthlyTrendResponse(
                        toString(row[0]),
                        toBigDecimal(row[1]),
                        toBigDecimal(row[2])
                ))
                .toList();

        // 예산 및 요약 정보
        var budgetSummary = budgetService.getMonthlyBudgetSummary(userId, yearMonth);

        // 요약 정보
        SummaryResponse summary = new SummaryResponse(budgetSummary.totalActualExpenseSum());

        return new DashboardResponse(categoryExpenses, trends, new BudgetStatusResponse(budgetSummary), summary);
    }

    private static String toString(Object value) {
        return value == null ? "" : value.toString();
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(value.toString());
    }
}
