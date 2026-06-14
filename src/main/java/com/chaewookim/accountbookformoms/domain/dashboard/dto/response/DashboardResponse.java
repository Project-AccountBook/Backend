package com.chaewookim.accountbookformoms.domain.dashboard.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DashboardResponse(

        Map<String, BigDecimal> categoryExpenses, // 카테고리별 지출
        List<MonthlyTrendResponse> trends,                // 최근 6개월 추이
        BudgetStatusResponse budgetStatus,                // 예산 상태
        SummaryResponse summary                           // 요약 (총지출, 전월비 등)
) {
}
