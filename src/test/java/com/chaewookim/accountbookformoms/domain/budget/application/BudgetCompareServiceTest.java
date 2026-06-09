package com.chaewookim.accountbookformoms.domain.budget.application;

import com.chaewookim.accountbookformoms.domain.asset.entity.TransactionCategory;
import com.chaewookim.accountbookformoms.domain.budget.dao.BudgetRepository;
import com.chaewookim.accountbookformoms.domain.budget.dto.request.BudgetCompareRequest;
import com.chaewookim.accountbookformoms.domain.budget.dto.request.PublicBudgetFilterRequest;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.BudgetCompareResponse;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.MyBudgetResponse;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.PublicMonthlyBudgetResponse;
import com.chaewookim.accountbookformoms.domain.budget.entity.Budget;
import com.chaewookim.accountbookformoms.domain.budget.enums.BudgetCompareType;
import com.chaewookim.accountbookformoms.domain.budget.error.BudgetErrorCode;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.domain.user.error.UserErrorCode;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class BudgetCompareServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BudgetCompareService budgetCompareService;

    private Budget budget(BigDecimal total, BigDecimal expected, TransactionCategory category) {
        return Budget.builder()
                .transactionCategory(category)
                .yearMonth("2026-06")
                .totalBudget(total)
                .expectedExpense(expected)
                .build();
    }

    @Test
    @DisplayName("내 월별 예산 조회 - 총 예산 합산 및 카테고리 항목 반환")
    void getMyMonthlyBudget_success() {

        // given
        TransactionCategory category = TransactionCategory.builder().name("식비").build();
        given(budgetRepository.findByUserIdAndYearMonth(1L, "2026-06"))
                .willReturn(List.of(
                        budget(new BigDecimal("300000"), new BigDecimal("200000"), category),
                        budget(new BigDecimal("100000"), new BigDecimal("50000"), null)
                ));

        // when
        MyBudgetResponse response = budgetCompareService.getMyMonthlyBudget(1L, "2026-06");

        // then
        assertThat(response.yearMonth()).isEqualTo("2026-06");
        assertThat(response.totalBudget()).isEqualByComparingTo("400000");
        assertThat(response.categoryBudgets()).hasSize(2);
    }

    @Test
    @DisplayName("내 월별 예산 조회 - 잘못된 yearMonth 포맷 예외")
    void getMyMonthlyBudget_invalid_year_month() {

        // when & then
        assertThatThrownBy(() -> budgetCompareService.getMyMonthlyBudget(1L, "2026/06"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", BudgetErrorCode.INVALID_YEAR_MONTH);
    }

    @Test
    @DisplayName("공개 사용자 월별 총 예산 - 연월/금액 필터 적용")
    void getPublicMonthlyBudgets_success() {

        // given
        PublicBudgetFilterRequest filter = new PublicBudgetFilterRequest(
                2026, 2026, 1, 6,
                new BigDecimal("100000"), new BigDecimal("1000000"));

        given(budgetRepository.findPublicMonthlyTotals(
                eq("2026-01"), eq("2026-06"), any(BigDecimal.class), any(BigDecimal.class)))
                .willReturn(List.of(
                        new Object[]{10L, "alice", "2026-06", new BigDecimal("500000")},
                        new Object[]{20L, "bob", "2026-05", new BigDecimal("300000")}
                ));

        // when
        List<PublicMonthlyBudgetResponse> result = budgetCompareService.getPublicMonthlyBudgets(filter);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).username()).isEqualTo("alice");
        assertThat(result.get(0).totalBudget()).isEqualByComparingTo("500000");
    }

    @Test
    @DisplayName("공개 사용자 목록 - 금액 구간 역전 시 예외")
    void getPublicMonthlyBudgets_invalid_amount_range() {

        // given
        PublicBudgetFilterRequest filter = new PublicBudgetFilterRequest(
                null, null, null, null,
                new BigDecimal("1000000"), new BigDecimal("500000"));

        // when & then
        assertThatThrownBy(() -> budgetCompareService.getPublicMonthlyBudgets(filter))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", BudgetErrorCode.INVALID_AMOUNT_RANGE);
    }

    @Test
    @DisplayName("AGE 비교 - 본인 나이대 사용자들 평균과 본인 합계 비교")
    void compareWithGroup_age_success() {

        // given
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        given(user.getBirthDate()).willReturn(LocalDate.of(LocalDate.now().getYear() - 35, 5, 1));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(budgetRepository.sumMonthlyTotalsByAgeRange(anyString(), any(LocalDate.class), any(LocalDate.class), anyLong()))
                .willReturn(List.of(
                        new Object[]{2L, new BigDecimal("400000")},
                        new Object[]{3L, new BigDecimal("600000")}
                ));
        given(budgetRepository.sumTotalBudgetByUserIdAndYearMonth(1L, "2026-06"))
                .willReturn(new BigDecimal("700000"));

        // when
        BudgetCompareResponse response = budgetCompareService.compareWithGroup(
                1L, new BudgetCompareRequest(BudgetCompareType.AGE, "2026-06", null, null, null));

        // then
        assertThat(response.type()).isEqualTo(BudgetCompareType.AGE);
        assertThat(response.myAmount()).isEqualByComparingTo("700000");
        assertThat(response.averageAmount()).isEqualByComparingTo("500000");
        assertThat(response.sampleSize()).isEqualTo(2);
        assertThat(response.difference()).isEqualByComparingTo("200000");
    }

    @Test
    @DisplayName("AGE 비교 - 생년월일 없는 사용자 예외")
    void compareWithGroup_age_birth_date_required() {

        // given
        User user = mock(User.class);
        given(user.getBirthDate()).willReturn(null);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> budgetCompareService.compareWithGroup(
                1L, new BudgetCompareRequest(BudgetCompareType.AGE, "2026-06", null, null, null)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", BudgetErrorCode.BIRTH_DATE_REQUIRED);
    }

    @Test
    @DisplayName("AMOUNT 비교 - 표본 없을 때 평균 0 반환")
    void compareWithGroup_amount_empty_group() {

        // given
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(budgetRepository.sumMonthlyTotalsByAmountRange(anyString(), any(), any(), anyLong()))
                .willReturn(List.of());
        given(budgetRepository.sumTotalBudgetByUserIdAndYearMonth(1L, "2026-06"))
                .willReturn(new BigDecimal("250000"));

        // when
        BudgetCompareResponse response = budgetCompareService.compareWithGroup(
                1L, new BudgetCompareRequest(BudgetCompareType.AMOUNT, "2026-06",
                        new BigDecimal("100000"), new BigDecimal("500000"), null));

        // then
        assertThat(response.averageAmount()).isEqualByComparingTo("0");
        assertThat(response.sampleSize()).isZero();
        assertThat(response.myAmount()).isEqualByComparingTo("250000");
    }

    @Test
    @DisplayName("CATEGORY 비교 - 같은 카테고리 평균과 본인 카테고리 예산 비교")
    void compareWithGroup_category_success() {

        // given
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(budgetRepository.averageCategoryBudget(anyString(), anyLong(), anyLong()))
                .willReturn(new Object[]{new BigDecimal("250000"), 4L});
        given(budgetRepository.findMyCategoryBudget(anyLong(), anyString(), anyLong()))
                .willReturn(new BigDecimal("300000"));

        // when
        BudgetCompareResponse response = budgetCompareService.compareWithGroup(
                1L, new BudgetCompareRequest(BudgetCompareType.CATEGORY, "2026-06", null, null, 10L));

        // then
        assertThat(response.type()).isEqualTo(BudgetCompareType.CATEGORY);
        assertThat(response.myAmount()).isEqualByComparingTo("300000");
        assertThat(response.averageAmount()).isEqualByComparingTo("250000");
        assertThat(response.sampleSize()).isEqualTo(4);
        assertThat(response.difference()).isEqualByComparingTo("50000");
    }

    @Test
    @DisplayName("CATEGORY 비교 - categoryId 없으면 예외")
    void compareWithGroup_category_id_required() {

        // given
        User user = mock(User.class);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> budgetCompareService.compareWithGroup(
                1L, new BudgetCompareRequest(BudgetCompareType.CATEGORY, "2026-06", null, null, null)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", BudgetErrorCode.CATEGORY_ID_REQUIRED);
    }

    @Test
    @DisplayName("비교 - 사용자 없을 시 예외")
    void compareWithGroup_user_not_found() {

        // given
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> budgetCompareService.compareWithGroup(
                99L, new BudgetCompareRequest(BudgetCompareType.AGE, "2026-06", null, null, null)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
    }
}
