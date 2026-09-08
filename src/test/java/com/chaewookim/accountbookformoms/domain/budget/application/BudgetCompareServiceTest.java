package com.chaewookim.accountbookformoms.domain.budget.application;

import com.chaewookim.accountbookformoms.domain.asset.entity.TransactionCategory;
import com.chaewookim.accountbookformoms.domain.budget.dao.BudgetRepository;
import com.chaewookim.accountbookformoms.domain.budget.dto.request.BudgetCompareRequest;
import com.chaewookim.accountbookformoms.domain.budget.dto.request.PublicBudgetFilterRequest;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.BudgetCompareResponse;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.MyBudgetResponse;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.PairBudgetDetailResponse;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.PublicMonthlyBudgetResponse;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.UserBudgetCompareResponse;
import com.chaewookim.accountbookformoms.domain.budget.entity.Budget;
import com.chaewookim.accountbookformoms.domain.user.entity.UserSetting;
import com.chaewookim.accountbookformoms.domain.budget.enums.BudgetCompareType;
import com.chaewookim.accountbookformoms.domain.budget.error.BudgetErrorCode;
import com.chaewookim.accountbookformoms.domain.budget.application.BudgetGroupCacheService.CategoryAggregation;
import com.chaewookim.accountbookformoms.domain.user.application.UserLocationService;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class BudgetCompareServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserLocationService userLocationService;

    @Mock
    private BudgetGroupCacheService groupCache;

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
                2026, 6,
                new BigDecimal("100000"), new BigDecimal("1000000"));

        given(budgetRepository.findPublicMonthlyTotals(
                eq("2026-06"),
                eq(LocalDate.of(2026, 6, 1)),
                eq(LocalDate.of(2026, 6, 30)),
                any(BigDecimal.class), any(BigDecimal.class)))
                .willReturn(List.of(
                        new Object[]{10L, "alice", "2026-06", new BigDecimal("500000")},
                        new Object[]{20L, "bob", "2026-06", new BigDecimal("0")}
                ));

        // when
        List<PublicMonthlyBudgetResponse> result = budgetCompareService.getPublicMonthlyBudgets(filter);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).username()).isEqualTo("alice");
        assertThat(result.get(0).totalBudget()).isEqualByComparingTo("500000");
        assertThat(result.get(1).username()).isEqualTo("bob");
        assertThat(result.get(1).totalBudget()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("공개 사용자 목록 - 금액 구간 역전 시 예외")
    void getPublicMonthlyBudgets_invalid_amount_range() {

        // given
        PublicBudgetFilterRequest filter = new PublicBudgetFilterRequest(
                2026, 6,
                new BigDecimal("1000000"), new BigDecimal("500000"));

        // when & then
        assertThatThrownBy(() -> budgetCompareService.getPublicMonthlyBudgets(filter))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", BudgetErrorCode.INVALID_AMOUNT_RANGE);
    }

    @Test
    @DisplayName("공개 사용자 목록 - year/month 누락 시 예외")
    void getPublicMonthlyBudgets_missing_year_month() {

        // given
        PublicBudgetFilterRequest filter = new PublicBudgetFilterRequest(
                null, null, null, null);

        // when & then
        assertThatThrownBy(() -> budgetCompareService.getPublicMonthlyBudgets(filter))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", BudgetErrorCode.INVALID_YEAR_MONTH);
    }

    @Test
    @DisplayName("AGE 비교 - 본인 나이대 사용자들 평균과 본인 합계 비교")
    void compareWithGroup_age_success() {

        // given
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        given(user.getBirthDate()).willReturn(LocalDate.of(LocalDate.now().getYear() - 35, 5, 1));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        // 그룹 캐시는 본인 포함 모든 공개 사용자 반환 → service 가 본인 제거
        given(groupCache.getAgeGroupSums(anyString(), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(Map.of(
                        1L, new BigDecimal("700000"),
                        2L, new BigDecimal("400000"),
                        3L, new BigDecimal("600000")
                ));
        given(budgetRepository.sumTotalBudgetByUserIdAndYearMonth(1L, "2026-06"))
                .willReturn(new BigDecimal("700000"));

        // when
        BudgetCompareResponse response = budgetCompareService.compareWithGroup(
                1L, new BudgetCompareRequest(BudgetCompareType.AGE, "2026-06", null, null, null, null));

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
                1L, new BudgetCompareRequest(BudgetCompareType.AGE, "2026-06", null, null, null, null)))
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
        given(groupCache.getAmountGroupSums(anyString(), any(), any()))
                .willReturn(Map.of());
        given(budgetRepository.sumTotalBudgetByUserIdAndYearMonth(1L, "2026-06"))
                .willReturn(new BigDecimal("250000"));

        // when
        BudgetCompareResponse response = budgetCompareService.compareWithGroup(
                1L, new BudgetCompareRequest(BudgetCompareType.AMOUNT, "2026-06",
                        new BigDecimal("100000"), new BigDecimal("500000"), null, null));

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
        // 캐시는 본인 포함. 다른 4명 평균=250000 + 본인 300000 → sumAll = 1300000, count=5
        // 서비스가 본인을 제외 → groupSum=1000000 / count=4 → avg 250000
        given(groupCache.getCategoryAggregation(anyString(), anyLong()))
                .willReturn(new CategoryAggregation(new BigDecimal("1300000"), 5L));
        given(budgetRepository.findMyCategoryBudget(anyLong(), anyString(), anyLong()))
                .willReturn(new BigDecimal("300000"));

        // when
        BudgetCompareResponse response = budgetCompareService.compareWithGroup(
                1L, new BudgetCompareRequest(BudgetCompareType.CATEGORY, "2026-06", null, null, 10L, null));

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
                1L, new BudgetCompareRequest(BudgetCompareType.CATEGORY, "2026-06", null, null, null, null)))
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
                99L, new BudgetCompareRequest(BudgetCompareType.AGE, "2026-06", null, null, null, null)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
    }

    private User publicUser(Long id, String username, LocalDate birthDate) {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(id);
        lenient().when(user.getUsername()).thenReturn(username);
        lenient().when(user.getBirthDate()).thenReturn(birthDate);
        UserSetting setting = mock(UserSetting.class);
        lenient().when(setting.getIsPortfolioPublic()).thenReturn(true);
        lenient().when(user.getUserSetting()).thenReturn(setting);
        return user;
    }

    @Test
    @DisplayName("두 사용자 예산 세부 조회 - 본인+대상자 월 총/카테고리별 예산 반환")
    void getPairBudgetDetails_success() {

        // given
        User me = publicUser(1L, "me", LocalDate.of(1990, 1, 1));
        User target = publicUser(2L, "target", LocalDate.of(1992, 3, 1));

        given(userRepository.findById(1L)).willReturn(Optional.of(me));
        given(userRepository.findById(2L)).willReturn(Optional.of(target));

        TransactionCategory food = TransactionCategory.builder().name("식비").build();
        given(budgetRepository.findByUserIdAndYearMonth(1L, "2026-06"))
                .willReturn(List.of(budget(new BigDecimal("200000"), new BigDecimal("100000"), food)));
        given(budgetRepository.findByUserIdAndYearMonth(2L, "2026-06"))
                .willReturn(List.of(
                        budget(new BigDecimal("300000"), new BigDecimal("200000"), food),
                        budget(new BigDecimal("100000"), new BigDecimal("80000"), null)));

        // when
        PairBudgetDetailResponse response = budgetCompareService.getPairBudgetDetails(1L, 2L, "2026-06");

        // then
        assertThat(response.me().userId()).isEqualTo(1L);
        assertThat(response.me().totalBudget()).isEqualByComparingTo("200000");
        assertThat(response.me().categoryBudgets()).hasSize(1);
        assertThat(response.target().userId()).isEqualTo(2L);
        assertThat(response.target().totalBudget()).isEqualByComparingTo("400000");
        assertThat(response.target().categoryBudgets()).hasSize(2);
    }

    @Test
    @DisplayName("두 사용자 세부 조회 - 자기 자신 비교 예외")
    void getPairBudgetDetails_self_compare_forbidden() {

        // when & then
        assertThatThrownBy(() -> budgetCompareService.getPairBudgetDetails(1L, 1L, "2026-06"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", BudgetErrorCode.CANNOT_COMPARE_SELF);
    }

    @Test
    @DisplayName("두 사용자 세부 조회 - 비공개 사용자는 예외")
    void getPairBudgetDetails_target_not_public() {

        // given
        User me = mock(User.class);
        User target = mock(User.class);
        UserSetting setting = mock(UserSetting.class);
        given(setting.getIsPortfolioPublic()).willReturn(false);
        given(target.getUserSetting()).willReturn(setting);
        given(userRepository.findById(1L)).willReturn(Optional.of(me));
        given(userRepository.findById(2L)).willReturn(Optional.of(target));

        // when & then
        assertThatThrownBy(() -> budgetCompareService.getPairBudgetDetails(1L, 2L, "2026-06"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", BudgetErrorCode.TARGET_NOT_PUBLIC);
    }

    @Test
    @DisplayName("선택 사용자 비교 - AMOUNT 모드: 월 총 예산 직접 비교")
    void compareWithUser_amount_success() {

        // given
        User me = publicUser(1L, "me", null);
        User target = publicUser(2L, "target", null);
        given(userRepository.findById(1L)).willReturn(Optional.of(me));
        given(userRepository.findById(2L)).willReturn(Optional.of(target));
        given(budgetRepository.sumTotalBudgetByUserIdAndYearMonth(1L, "2026-06"))
                .willReturn(new BigDecimal("500000"));
        given(budgetRepository.sumTotalBudgetByUserIdAndYearMonth(2L, "2026-06"))
                .willReturn(new BigDecimal("700000"));

        // when
        UserBudgetCompareResponse response = budgetCompareService.compareWithUser(
                1L, 2L, BudgetCompareType.AMOUNT, "2026-06", null);

        // then
        assertThat(response.type()).isEqualTo(BudgetCompareType.AMOUNT);
        assertThat(response.myAmount()).isEqualByComparingTo("500000");
        assertThat(response.targetAmount()).isEqualByComparingTo("700000");
        assertThat(response.difference()).isEqualByComparingTo("-200000");
    }

    @Test
    @DisplayName("선택 사용자 비교 - AGE 모드: 나이대 라벨 동반")
    void compareWithUser_age_success() {

        // given
        int year = LocalDate.now().getYear();
        User me = publicUser(1L, "me", LocalDate.of(year - 35, 1, 1));     // 30대
        User target = publicUser(2L, "target", LocalDate.of(year - 42, 1, 1)); // 40대
        given(userRepository.findById(1L)).willReturn(Optional.of(me));
        given(userRepository.findById(2L)).willReturn(Optional.of(target));
        given(budgetRepository.sumTotalBudgetByUserIdAndYearMonth(1L, "2026-06"))
                .willReturn(new BigDecimal("400000"));
        given(budgetRepository.sumTotalBudgetByUserIdAndYearMonth(2L, "2026-06"))
                .willReturn(new BigDecimal("600000"));

        // when
        UserBudgetCompareResponse response = budgetCompareService.compareWithUser(
                1L, 2L, BudgetCompareType.AGE, "2026-06", null);

        // then
        assertThat(response.myLabel()).isEqualTo("30대");
        assertThat(response.targetLabel()).isEqualTo("40대");
        assertThat(response.difference()).isEqualByComparingTo("-200000");
    }

    @Test
    @DisplayName("선택 사용자 비교 - CATEGORY 모드: 카테고리별 예산 비교")
    void compareWithUser_category_success() {

        // given
        User me = publicUser(1L, "me", null);
        User target = publicUser(2L, "target", null);
        given(userRepository.findById(1L)).willReturn(Optional.of(me));
        given(userRepository.findById(2L)).willReturn(Optional.of(target));
        given(budgetRepository.findMyCategoryBudget(1L, "2026-06", 10L))
                .willReturn(new BigDecimal("200000"));
        given(budgetRepository.findMyCategoryBudget(2L, "2026-06", 10L))
                .willReturn(new BigDecimal("150000"));

        // when
        UserBudgetCompareResponse response = budgetCompareService.compareWithUser(
                1L, 2L, BudgetCompareType.CATEGORY, "2026-06", 10L);

        // then
        assertThat(response.type()).isEqualTo(BudgetCompareType.CATEGORY);
        assertThat(response.myAmount()).isEqualByComparingTo("200000");
        assertThat(response.targetAmount()).isEqualByComparingTo("150000");
        assertThat(response.difference()).isEqualByComparingTo("50000");
    }

    @Test
    @DisplayName("선택 사용자 비교 - CATEGORY 모드 categoryId 없으면 예외")
    void compareWithUser_category_id_required() {

        // given
        User me = publicUser(1L, "me", null);
        User target = publicUser(2L, "target", null);
        given(userRepository.findById(1L)).willReturn(Optional.of(me));
        given(userRepository.findById(2L)).willReturn(Optional.of(target));

        // when & then
        assertThatThrownBy(() -> budgetCompareService.compareWithUser(
                1L, 2L, BudgetCompareType.CATEGORY, "2026-06", null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", BudgetErrorCode.CATEGORY_ID_REQUIRED);
    }

    @Test
    @DisplayName("선택 사용자 비교 - AGE 모드: 생년월일 없으면 예외")
    void compareWithUser_age_birth_required() {

        // given
        User me = publicUser(1L, "me", null);
        User target = publicUser(2L, "target", LocalDate.of(1990, 1, 1));
        given(userRepository.findById(1L)).willReturn(Optional.of(me));
        given(userRepository.findById(2L)).willReturn(Optional.of(target));

        // when & then
        assertThatThrownBy(() -> budgetCompareService.compareWithUser(
                1L, 2L, BudgetCompareType.AGE, "2026-06", null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", BudgetErrorCode.BIRTH_DATE_REQUIRED);
    }

    @Test
    @DisplayName("그룹 평균 비교 - LOCATION: 반경 내 공개 사용자 그룹 평균 + 본인 차이")
    void compareWithGroup_location_success() {

        // given
        User me = publicUser(1L, "me", null);
        given(userRepository.findById(1L)).willReturn(Optional.of(me));
        given(userLocationService.findNearbyUserIds(1L, 3.0)).willReturn(Set.of(2L, 3L));
        given(budgetRepository.sumMonthlyTotalsByUserIds(eq("2026-06"), any()))
                .willReturn(List.<Object[]>of(
                        new Object[]{2L, new BigDecimal("400000")},
                        new Object[]{3L, new BigDecimal("800000")}));
        given(budgetRepository.sumTotalBudgetByUserIdAndYearMonth(1L, "2026-06"))
                .willReturn(new BigDecimal("500000"));

        // when
        BudgetCompareResponse response = budgetCompareService.compareWithGroup(
                1L, new BudgetCompareRequest(BudgetCompareType.LOCATION, "2026-06", null, null, null, 3.0));

        // then
        assertThat(response.type()).isEqualTo(BudgetCompareType.LOCATION);
        assertThat(response.sampleSize()).isEqualTo(2L);
        assertThat(response.averageAmount()).isEqualByComparingTo("600000");
        assertThat(response.myAmount()).isEqualByComparingTo("500000");
        assertThat(response.difference()).isEqualByComparingTo("-100000");
    }

    @Test
    @DisplayName("그룹 평균 비교 - LOCATION: 반경 누락 시 예외")
    void compareWithGroup_location_radius_required() {

        // given
        User me = publicUser(1L, "me", null);
        given(userRepository.findById(1L)).willReturn(Optional.of(me));

        // when & then
        assertThatThrownBy(() -> budgetCompareService.compareWithGroup(
                1L, new BudgetCompareRequest(BudgetCompareType.LOCATION, "2026-06", null, null, null, null)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", BudgetErrorCode.RADIUS_REQUIRED);
    }

    @Test
    @DisplayName("그룹 평균 비교 - LOCATION: 반경 0.1km 미만이면 예외")
    void compareWithGroup_location_radius_too_small() {

        // given
        User me = publicUser(1L, "me", null);
        given(userRepository.findById(1L)).willReturn(Optional.of(me));

        // when & then
        assertThatThrownBy(() -> budgetCompareService.compareWithGroup(
                1L, new BudgetCompareRequest(BudgetCompareType.LOCATION, "2026-06", null, null, null, 0.05)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", BudgetErrorCode.INVALID_RADIUS);
    }

    @Test
    @DisplayName("그룹 평균 비교 - LOCATION: 반경 50km 초과면 예외")
    void compareWithGroup_location_radius_too_large() {

        // given
        User me = publicUser(1L, "me", null);
        given(userRepository.findById(1L)).willReturn(Optional.of(me));

        // when & then
        assertThatThrownBy(() -> budgetCompareService.compareWithGroup(
                1L, new BudgetCompareRequest(BudgetCompareType.LOCATION, "2026-06", null, null, null, 100.0)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", BudgetErrorCode.INVALID_RADIUS);
    }

    @Test
    @DisplayName("그룹 평균 비교 - LOCATION: 반경 내 사용자 0명일 때 sampleSize=0, average=0")
    void compareWithGroup_location_no_neighbors() {

        // given
        User me = publicUser(1L, "me", null);
        given(userRepository.findById(1L)).willReturn(Optional.of(me));
        given(userLocationService.findNearbyUserIds(1L, 1.0)).willReturn(Set.of());
        given(budgetRepository.sumTotalBudgetByUserIdAndYearMonth(1L, "2026-06"))
                .willReturn(new BigDecimal("500000"));

        // when
        BudgetCompareResponse response = budgetCompareService.compareWithGroup(
                1L, new BudgetCompareRequest(BudgetCompareType.LOCATION, "2026-06", null, null, null, 1.0));

        // then
        assertThat(response.sampleSize()).isZero();
        assertThat(response.averageAmount()).isEqualByComparingTo("0");
        assertThat(response.myAmount()).isEqualByComparingTo("500000");
    }

    @Test
    @DisplayName("선택 사용자 비교 - LOCATION 은 1:1 비교 미지원 예외")
    void compareWithUser_location_not_supported() {

        // given
        User me = publicUser(1L, "me", null);
        User target = publicUser(2L, "target", null);
        given(userRepository.findById(1L)).willReturn(Optional.of(me));
        given(userRepository.findById(2L)).willReturn(Optional.of(target));

        // when & then
        assertThatThrownBy(() -> budgetCompareService.compareWithUser(
                1L, 2L, BudgetCompareType.LOCATION, "2026-06", null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", BudgetErrorCode.LOCATION_PAIR_NOT_SUPPORTED);
    }
}
