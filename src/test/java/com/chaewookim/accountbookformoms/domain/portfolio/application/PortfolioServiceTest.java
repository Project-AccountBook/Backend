package com.chaewookim.accountbookformoms.domain.portfolio.application;

import com.chaewookim.accountbookformoms.domain.budget.application.BudgetCompareService;
import com.chaewookim.accountbookformoms.domain.budget.dto.request.BudgetCompareRequest;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.BudgetCompareResponse;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.MyBudgetResponse;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.PairBudgetDetailResponse;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.UserBudgetCompareResponse;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.UserBudgetDetailResponse;
import com.chaewookim.accountbookformoms.domain.budget.enums.BudgetCompareType;
import com.chaewookim.accountbookformoms.domain.expense.application.ExpenseCompareService;
import com.chaewookim.accountbookformoms.domain.expense.dto.request.ExpenseCompareRequest;
import com.chaewookim.accountbookformoms.domain.expense.dto.response.ExpenseCompareResponse;
import com.chaewookim.accountbookformoms.domain.expense.dto.response.MyExpenseResponse;
import com.chaewookim.accountbookformoms.domain.expense.dto.response.PairExpenseDetailResponse;
import com.chaewookim.accountbookformoms.domain.expense.dto.response.UserExpenseCompareResponse;
import com.chaewookim.accountbookformoms.domain.expense.dto.response.UserExpenseDetailResponse;
import com.chaewookim.accountbookformoms.domain.expense.enums.ExpenseCompareType;
import com.chaewookim.accountbookformoms.domain.income.application.IncomeCompareService;
import com.chaewookim.accountbookformoms.domain.income.dto.request.IncomeCompareRequest;
import com.chaewookim.accountbookformoms.domain.income.dto.response.IncomeCompareResponse;
import com.chaewookim.accountbookformoms.domain.income.dto.response.MyIncomeResponse;
import com.chaewookim.accountbookformoms.domain.income.dto.response.PairIncomeDetailResponse;
import com.chaewookim.accountbookformoms.domain.income.dto.response.UserIncomeCompareResponse;
import com.chaewookim.accountbookformoms.domain.income.dto.response.UserIncomeDetailResponse;
import com.chaewookim.accountbookformoms.domain.income.enums.IncomeCompareType;
import com.chaewookim.accountbookformoms.domain.portfolio.dao.PortfolioAggregationRepository;
import com.chaewookim.accountbookformoms.domain.portfolio.dao.PortfolioAggregationRepository.PublicPortfolioRow;
import com.chaewookim.accountbookformoms.domain.portfolio.dto.request.PortfolioCompareRequest;
import com.chaewookim.accountbookformoms.domain.portfolio.dto.request.PortfolioFilterRequest;
import com.chaewookim.accountbookformoms.domain.portfolio.dto.response.MyPortfolioResponse;
import com.chaewookim.accountbookformoms.domain.portfolio.dto.response.PairPortfolioDetailResponse;
import com.chaewookim.accountbookformoms.domain.portfolio.dto.response.PortfolioCompareResponse;
import com.chaewookim.accountbookformoms.domain.portfolio.dto.response.PublicMonthlyPortfolioResponse;
import com.chaewookim.accountbookformoms.domain.portfolio.dto.response.UserPortfolioCompareResponse;
import com.chaewookim.accountbookformoms.domain.portfolio.enums.PortfolioCompareType;
import com.chaewookim.accountbookformoms.domain.portfolio.error.PortfolioErrorCode;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private IncomeCompareService incomeCompareService;

    @Mock
    private ExpenseCompareService expenseCompareService;

    @Mock
    private BudgetCompareService budgetCompareService;

    @Mock
    private PortfolioAggregationRepository portfolioAggregationRepository;

    @InjectMocks
    private PortfolioService portfolioService;

    private MyIncomeResponse myIncome(String yearMonth, String total) {
        return new MyIncomeResponse(yearMonth, new BigDecimal(total),
                BigDecimal.ZERO, new BigDecimal(total), List.of(), List.of());
    }

    private MyExpenseResponse myExpense(String yearMonth, String total) {
        return new MyExpenseResponse(yearMonth, new BigDecimal(total),
                BigDecimal.ZERO, new BigDecimal(total), List.of(), List.of());
    }

    private MyBudgetResponse myBudget(String yearMonth, String total) {
        return new MyBudgetResponse(yearMonth, new BigDecimal(total), List.of());
    }

    @Test
    @DisplayName("내 포트폴리오 조회 - 수입/지출/예산 합산 및 잔액(=수입-지출) 반환")
    void getMyPortfolio_success() {

        // given
        given(incomeCompareService.getMyMonthlyIncome(1L, "2026-06"))
                .willReturn(myIncome("2026-06", "5000000"));
        given(expenseCompareService.getMyMonthlyExpense(1L, "2026-06"))
                .willReturn(myExpense("2026-06", "3000000"));
        given(budgetCompareService.getMyMonthlyBudget(1L, "2026-06"))
                .willReturn(myBudget("2026-06", "3500000"));

        // when
        MyPortfolioResponse response = portfolioService.getMyPortfolio(1L, "2026-06");

        // then
        assertThat(response.yearMonth()).isEqualTo("2026-06");
        assertThat(response.totalIncome()).isEqualByComparingTo("5000000");
        assertThat(response.totalExpense()).isEqualByComparingTo("3000000");
        assertThat(response.totalBudget()).isEqualByComparingTo("3500000");
        assertThat(response.balance()).isEqualByComparingTo("2000000");
    }

    @Test
    @DisplayName("내 포트폴리오 조회 - 잘못된 yearMonth 포맷 예외")
    void getMyPortfolio_invalid_year_month() {

        // when & then
        assertThatThrownBy(() -> portfolioService.getMyPortfolio(1L, "2026/06"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PortfolioErrorCode.INVALID_YEAR_MONTH);
    }

    @Test
    @DisplayName("공개 사용자 포트폴리오 목록 - 통합 집계 결과를 응답으로 매핑하고 잔액(=수입-지출)을 계산")
    void getPublicPortfolios_merge_users() {

        // given
        PortfolioFilterRequest filter = new PortfolioFilterRequest(
                2026, 6, null, null, null, null, null, null);

        given(portfolioAggregationRepository.findPublicPortfolios(
                eq(LocalDate.of(2026, 6, 1)),
                eq(LocalDate.of(2026, 6, 30)),
                eq("2026-06"),
                eq(null), eq(null), eq(null), eq(null), eq(null), eq(null)))
                .willReturn(List.of(
                        new PublicPortfolioRow(10L, "alice",
                                new BigDecimal("5000000"), new BigDecimal("2000000"), new BigDecimal("3000000")),
                        new PublicPortfolioRow(20L, "bob",
                                new BigDecimal("4000000"), BigDecimal.ZERO, new BigDecimal("2500000")),
                        new PublicPortfolioRow(30L, "carol",
                                BigDecimal.ZERO, new BigDecimal("1500000"), BigDecimal.ZERO)));

        // when
        List<PublicMonthlyPortfolioResponse> result = portfolioService.getPublicPortfolios(filter);

        // then
        assertThat(result).extracting(PublicMonthlyPortfolioResponse::userId)
                .containsExactly(10L, 20L, 30L);

        PublicMonthlyPortfolioResponse alice = result.get(0);
        assertThat(alice.totalIncome()).isEqualByComparingTo("5000000");
        assertThat(alice.totalExpense()).isEqualByComparingTo("2000000");
        assertThat(alice.totalBudget()).isEqualByComparingTo("3000000");
        assertThat(alice.balance()).isEqualByComparingTo("3000000");

        PublicMonthlyPortfolioResponse bob = result.get(1);
        assertThat(bob.totalExpense()).isEqualByComparingTo("0");
        assertThat(bob.balance()).isEqualByComparingTo("4000000");

        PublicMonthlyPortfolioResponse carol = result.get(2);
        assertThat(carol.totalIncome()).isEqualByComparingTo("0");
        assertThat(carol.totalBudget()).isEqualByComparingTo("0");
        assertThat(carol.balance()).isEqualByComparingTo("-1500000");
    }

    @Test
    @DisplayName("공개 사용자 포트폴리오 목록 - 금액 구간 필터를 그대로 Repository 에 전달")
    void getPublicPortfolios_filter_by_amount_ranges() {

        // given
        PortfolioFilterRequest filter = new PortfolioFilterRequest(
                2026, 6,
                new BigDecimal("4500000"), null,
                null, new BigDecimal("2500000"),
                null, null);

        given(portfolioAggregationRepository.findPublicPortfolios(
                eq(LocalDate.of(2026, 6, 1)),
                eq(LocalDate.of(2026, 6, 30)),
                eq("2026-06"),
                eq(new BigDecimal("4500000")), eq(null),
                eq(null), eq(new BigDecimal("2500000")),
                eq(null), eq(null)))
                .willReturn(List.of(
                        new PublicPortfolioRow(10L, "alice",
                                new BigDecimal("5000000"), new BigDecimal("2000000"), BigDecimal.ZERO)));

        // when
        List<PublicMonthlyPortfolioResponse> result = portfolioService.getPublicPortfolios(filter);

        // then
        assertThat(result).extracting(PublicMonthlyPortfolioResponse::userId).containsExactly(10L);
        verify(portfolioAggregationRepository).findPublicPortfolios(
                eq(LocalDate.of(2026, 6, 1)),
                eq(LocalDate.of(2026, 6, 30)),
                eq("2026-06"),
                eq(new BigDecimal("4500000")), eq(null),
                eq(null), eq(new BigDecimal("2500000")),
                eq(null), eq(null));
    }

    @Test
    @DisplayName("공개 사용자 목록 - 연/월 누락 시 예외")
    void getPublicPortfolios_year_month_required() {

        // given
        PortfolioFilterRequest filter = new PortfolioFilterRequest(
                2026, null, null, null, null, null, null, null);

        // when & then
        assertThatThrownBy(() -> portfolioService.getPublicPortfolios(filter))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PortfolioErrorCode.INVALID_YEAR_MONTH);
    }

    @Test
    @DisplayName("공개 사용자 목록 - 금액 구간 역전 시 예외")
    void getPublicPortfolios_invalid_amount_range() {

        // given
        PortfolioFilterRequest filter = new PortfolioFilterRequest(
                2026, 6, null, null,
                new BigDecimal("3000000"), new BigDecimal("1000000"),
                null, null);

        // when & then
        assertThatThrownBy(() -> portfolioService.getPublicPortfolios(filter))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PortfolioErrorCode.INVALID_AMOUNT_RANGE);
    }

    @Test
    @DisplayName("그룹 평균 비교 - 탭별 수입/지출/예산 비교 결과를 묶어 반환하고 각 도메인 요청을 매핑")
    void compareWithGroup_delegates_with_mapped_type() {

        // given
        PortfolioCompareRequest request = new PortfolioCompareRequest(
                PortfolioCompareType.AMOUNT, "2026-06",
                new BigDecimal("1000000"), new BigDecimal("6000000"),
                new BigDecimal("500000"), new BigDecimal("4000000"),
                new BigDecimal("800000"), new BigDecimal("5000000"),
                null);

        IncomeCompareResponse incomeResp = IncomeCompareResponse.of(
                IncomeCompareType.AMOUNT, "2026-06",
                BigDecimal.ZERO, new BigDecimal("5000000"),
                BigDecimal.ZERO, new BigDecimal("4000000"), 3L);
        ExpenseCompareResponse expenseResp = ExpenseCompareResponse.of(
                ExpenseCompareType.AMOUNT, "2026-06",
                BigDecimal.ZERO, new BigDecimal("3000000"),
                BigDecimal.ZERO, new BigDecimal("2500000"), 3L);
        BudgetCompareResponse budgetResp = BudgetCompareResponse.of(
                BudgetCompareType.AMOUNT, "2026-06",
                new BigDecimal("3500000"), new BigDecimal("3000000"), 3L);

        given(incomeCompareService.compareWithGroup(eq(1L), any(IncomeCompareRequest.class)))
                .willReturn(incomeResp);
        given(expenseCompareService.compareWithGroup(eq(1L), any(ExpenseCompareRequest.class)))
                .willReturn(expenseResp);
        given(budgetCompareService.compareWithGroup(eq(1L), any(BudgetCompareRequest.class)))
                .willReturn(budgetResp);

        // when
        PortfolioCompareResponse response = portfolioService.compareWithGroup(1L, request);

        // then - 묶인 응답
        assertThat(response.type()).isEqualTo(PortfolioCompareType.AMOUNT);
        assertThat(response.yearMonth()).isEqualTo("2026-06");
        assertThat(response.income()).isSameAs(incomeResp);
        assertThat(response.expense()).isSameAs(expenseResp);
        assertThat(response.budget()).isSameAs(budgetResp);

        // then - 각 도메인에 자신의 min/max 가 전달되었는지 검증
        ArgumentCaptor<IncomeCompareRequest> incomeCap = ArgumentCaptor.forClass(IncomeCompareRequest.class);
        verify(incomeCompareService).compareWithGroup(eq(1L), incomeCap.capture());
        assertThat(incomeCap.getValue().type()).isEqualTo(IncomeCompareType.AMOUNT);
        assertThat(incomeCap.getValue().minAmount()).isEqualByComparingTo("1000000");
        assertThat(incomeCap.getValue().maxAmount()).isEqualByComparingTo("6000000");

        ArgumentCaptor<ExpenseCompareRequest> expenseCap = ArgumentCaptor.forClass(ExpenseCompareRequest.class);
        verify(expenseCompareService).compareWithGroup(eq(1L), expenseCap.capture());
        assertThat(expenseCap.getValue().type()).isEqualTo(ExpenseCompareType.AMOUNT);
        assertThat(expenseCap.getValue().minAmount()).isEqualByComparingTo("500000");
        assertThat(expenseCap.getValue().maxAmount()).isEqualByComparingTo("4000000");

        ArgumentCaptor<BudgetCompareRequest> budgetCap = ArgumentCaptor.forClass(BudgetCompareRequest.class);
        verify(budgetCompareService).compareWithGroup(eq(1L), budgetCap.capture());
        assertThat(budgetCap.getValue().type()).isEqualTo(BudgetCompareType.AMOUNT);
        assertThat(budgetCap.getValue().minAmount()).isEqualByComparingTo("800000");
        assertThat(budgetCap.getValue().maxAmount()).isEqualByComparingTo("5000000");
    }

    @Test
    @DisplayName("그룹 평균 비교 - 금액 구간 역전 시 예외")
    void compareWithGroup_invalid_amount_range() {

        // given - 지출 min > max
        PortfolioCompareRequest request = new PortfolioCompareRequest(
                PortfolioCompareType.AMOUNT, "2026-06",
                null, null,
                new BigDecimal("3000000"), new BigDecimal("1000000"),
                null, null, null);

        // when & then
        assertThatThrownBy(() -> portfolioService.compareWithGroup(1L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PortfolioErrorCode.INVALID_AMOUNT_RANGE);
    }

    @Test
    @DisplayName("두 사용자 포트폴리오 세부 조회 - 본인+대상자 수입/지출/예산 세부 및 잔액 반환")
    void getPairPortfolioDetails_success() {

        // given
        UserIncomeDetailResponse myIncome = new UserIncomeDetailResponse(
                1L, "me", "2026-06",
                new BigDecimal("5000000"), BigDecimal.ZERO, new BigDecimal("5000000"),
                List.of(), List.of());
        UserIncomeDetailResponse targetIncome = new UserIncomeDetailResponse(
                2L, "target", "2026-06",
                new BigDecimal("4000000"), BigDecimal.ZERO, new BigDecimal("4000000"),
                List.of(), List.of());
        UserExpenseDetailResponse myExpense = new UserExpenseDetailResponse(
                1L, "me", "2026-06",
                new BigDecimal("2000000"), BigDecimal.ZERO, new BigDecimal("2000000"),
                List.of(), List.of());
        UserExpenseDetailResponse targetExpense = new UserExpenseDetailResponse(
                2L, "target", "2026-06",
                new BigDecimal("1500000"), BigDecimal.ZERO, new BigDecimal("1500000"),
                List.of(), List.of());
        UserBudgetDetailResponse myBudget = new UserBudgetDetailResponse(
                1L, "me", "2026-06", new BigDecimal("2500000"), List.of());
        UserBudgetDetailResponse targetBudget = new UserBudgetDetailResponse(
                2L, "target", "2026-06", new BigDecimal("2000000"), List.of());

        given(incomeCompareService.getPairIncomeDetails(1L, 2L, "2026-06"))
                .willReturn(new PairIncomeDetailResponse(myIncome, targetIncome));
        given(expenseCompareService.getPairExpenseDetails(1L, 2L, "2026-06"))
                .willReturn(new PairExpenseDetailResponse(myExpense, targetExpense));
        given(budgetCompareService.getPairBudgetDetails(1L, 2L, "2026-06"))
                .willReturn(new PairBudgetDetailResponse(myBudget, targetBudget));

        // when
        PairPortfolioDetailResponse response = portfolioService.getPairPortfolioDetails(1L, 2L, "2026-06");

        // then
        assertThat(response.me().userId()).isEqualTo(1L);
        assertThat(response.me().totalIncome()).isEqualByComparingTo("5000000");
        assertThat(response.me().totalExpense()).isEqualByComparingTo("2000000");
        assertThat(response.me().totalBudget()).isEqualByComparingTo("2500000");
        assertThat(response.me().balance()).isEqualByComparingTo("3000000");

        assertThat(response.target().userId()).isEqualTo(2L);
        assertThat(response.target().totalIncome()).isEqualByComparingTo("4000000");
        assertThat(response.target().totalExpense()).isEqualByComparingTo("1500000");
        assertThat(response.target().totalBudget()).isEqualByComparingTo("2000000");
        assertThat(response.target().balance()).isEqualByComparingTo("2500000");
    }

    @Test
    @DisplayName("두 사용자 세부 조회 - 자기 자신과는 비교 불가")
    void getPairPortfolioDetails_self_compare_forbidden() {

        // when & then
        assertThatThrownBy(() -> portfolioService.getPairPortfolioDetails(1L, 1L, "2026-06"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PortfolioErrorCode.CANNOT_COMPARE_SELF);
    }

    @Test
    @DisplayName("선택 사용자 비교 - 탭별 수입/지출/예산 비교 결과를 묶어 반환")
    void compareWithUser_success() {

        // given
        UserIncomeCompareResponse incomeResp = UserIncomeCompareResponse.of(
                IncomeCompareType.CATEGORY, "2026-06",
                1L, "카테고리 #10", BigDecimal.ZERO, new BigDecimal("3000000"),
                2L, "카테고리 #10", BigDecimal.ZERO, new BigDecimal("2500000"));
        UserExpenseCompareResponse expenseResp = UserExpenseCompareResponse.of(
                ExpenseCompareType.CATEGORY, "2026-06",
                1L, "카테고리 #10", BigDecimal.ZERO, new BigDecimal("1000000"),
                2L, "카테고리 #10", BigDecimal.ZERO, new BigDecimal("800000"));
        UserBudgetCompareResponse budgetResp = UserBudgetCompareResponse.of(
                BudgetCompareType.CATEGORY, "2026-06",
                1L, "카테고리 #10", new BigDecimal("1200000"),
                2L, "카테고리 #10", new BigDecimal("900000"));

        given(incomeCompareService.compareWithUser(1L, 2L, IncomeCompareType.CATEGORY, "2026-06", 10L))
                .willReturn(incomeResp);
        given(expenseCompareService.compareWithUser(1L, 2L, ExpenseCompareType.CATEGORY, "2026-06", 10L))
                .willReturn(expenseResp);
        given(budgetCompareService.compareWithUser(1L, 2L, BudgetCompareType.CATEGORY, "2026-06", 10L))
                .willReturn(budgetResp);

        // when
        UserPortfolioCompareResponse response = portfolioService.compareWithUser(
                1L, 2L, PortfolioCompareType.CATEGORY, "2026-06", 10L);

        // then
        assertThat(response.type()).isEqualTo(PortfolioCompareType.CATEGORY);
        assertThat(response.myUserId()).isEqualTo(1L);
        assertThat(response.targetUserId()).isEqualTo(2L);
        assertThat(response.income()).isSameAs(incomeResp);
        assertThat(response.expense()).isSameAs(expenseResp);
        assertThat(response.budget()).isSameAs(budgetResp);
    }

    @Test
    @DisplayName("선택 사용자 비교 - 자기 자신과는 비교 불가")
    void compareWithUser_self_compare_forbidden() {

        // when & then
        assertThatThrownBy(() -> portfolioService.compareWithUser(
                1L, 1L, PortfolioCompareType.AMOUNT, "2026-06", null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PortfolioErrorCode.CANNOT_COMPARE_SELF);
    }

    @Test
    @DisplayName("선택 사용자 비교 - 잘못된 yearMonth 포맷 예외")
    void compareWithUser_invalid_year_month() {

        // when & then
        assertThatThrownBy(() -> portfolioService.compareWithUser(
                1L, 2L, PortfolioCompareType.AMOUNT, "26-06", null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PortfolioErrorCode.INVALID_YEAR_MONTH);
    }
}
