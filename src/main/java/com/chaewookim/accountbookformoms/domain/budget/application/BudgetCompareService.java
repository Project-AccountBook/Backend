package com.chaewookim.accountbookformoms.domain.budget.application;

import com.chaewookim.accountbookformoms.domain.budget.dao.BudgetRepository;
import com.chaewookim.accountbookformoms.domain.budget.dto.request.BudgetCompareRequest;
import com.chaewookim.accountbookformoms.domain.budget.dto.request.PublicBudgetFilterRequest;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.BudgetCompareResponse;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.CategoryBudgetResponse;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.MyBudgetResponse;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.PairBudgetDetailResponse;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.PublicMonthlyBudgetResponse;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.UserBudgetCompareResponse;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.UserBudgetDetailResponse;
import com.chaewookim.accountbookformoms.domain.budget.entity.Budget;
import com.chaewookim.accountbookformoms.domain.budget.enums.BudgetCompareType;
import com.chaewookim.accountbookformoms.domain.budget.error.BudgetErrorCode;
import com.chaewookim.accountbookformoms.domain.user.application.UserLocationService;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.domain.user.error.UserErrorCode;
import com.chaewookim.accountbookformoms.global.config.RedisConfig;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BudgetCompareService {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final UserLocationService userLocationService;

    public MyBudgetResponse getMyMonthlyBudget(Long userId, String yearMonth) {

        validateYearMonth(yearMonth);

        List<Budget> budgets = budgetRepository.findByUserIdAndYearMonth(userId, yearMonth);
        List<CategoryBudgetResponse> categoryBudgets = budgets.stream()
                .map(CategoryBudgetResponse::from)
                .toList();

        BigDecimal total = budgets.stream()
                .map(Budget::getTotalBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new MyBudgetResponse(yearMonth, total, categoryBudgets);
    }

    public List<PublicMonthlyBudgetResponse> getPublicMonthlyBudgets(PublicBudgetFilterRequest filter) {

        validateAmountRange(filter.minAmount(), filter.maxAmount());

        String yearMonthFrom = buildBoundary(filter.yearFrom(), filter.monthFrom(), true);
        String yearMonthTo = buildBoundary(filter.yearTo(), filter.monthTo(), false);

        return budgetRepository.findPublicMonthlyTotals(
                        yearMonthFrom,
                        yearMonthTo,
                        filter.minAmount(),
                        filter.maxAmount())
                .stream()
                .map(row -> new PublicMonthlyBudgetResponse(
                        (Long) row[0],
                        (String) row[1],
                        (String) row[2],
                        toBigDecimal(row[3])))
                .toList();
    }

    public PairBudgetDetailResponse getPairBudgetDetails(Long myUserId, Long targetUserId, String yearMonth) {

        validateYearMonth(yearMonth);
        if (myUserId.equals(targetUserId)) {
            throw new CustomException(BudgetErrorCode.CANNOT_COMPARE_SELF);
        }

        User me = userRepository.findById(myUserId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        User target = loadPublicTarget(targetUserId);

        return new PairBudgetDetailResponse(
                toUserBudgetDetail(me, yearMonth),
                toUserBudgetDetail(target, yearMonth));
    }

    public UserBudgetCompareResponse compareWithUser(Long myUserId, Long targetUserId,
                                                     BudgetCompareType type, String yearMonth, Long categoryId) {

        validateYearMonth(yearMonth);
        if (myUserId.equals(targetUserId)) {
            throw new CustomException(BudgetErrorCode.CANNOT_COMPARE_SELF);
        }

        User me = userRepository.findById(myUserId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        User target = loadPublicTarget(targetUserId);

        return switch (type) {
            case AGE -> compareUsersByAge(me, target, yearMonth);
            case AMOUNT -> compareUsersByAmount(me, target, yearMonth);
            case CATEGORY -> compareUsersByCategory(me, target, yearMonth, categoryId);
            case LOCATION -> throw new CustomException(BudgetErrorCode.LOCATION_PAIR_NOT_SUPPORTED);
        };
    }

    private UserBudgetCompareResponse compareUsersByAge(User me, User target, String yearMonth) {
        if (me.getBirthDate() == null || target.getBirthDate() == null) {
            throw new CustomException(BudgetErrorCode.BIRTH_DATE_REQUIRED);
        }
        BigDecimal myAmount = nullToZero(budgetRepository.sumTotalBudgetByUserIdAndYearMonth(me.getId(), yearMonth));
        BigDecimal targetAmount = nullToZero(budgetRepository.sumTotalBudgetByUserIdAndYearMonth(target.getId(), yearMonth));
        return UserBudgetCompareResponse.of(
                BudgetCompareType.AGE, yearMonth,
                me.getId(), decadeLabel(me.getBirthDate()), myAmount,
                target.getId(), decadeLabel(target.getBirthDate()), targetAmount);
    }

    private UserBudgetCompareResponse compareUsersByAmount(User me, User target, String yearMonth) {
        BigDecimal myAmount = nullToZero(budgetRepository.sumTotalBudgetByUserIdAndYearMonth(me.getId(), yearMonth));
        BigDecimal targetAmount = nullToZero(budgetRepository.sumTotalBudgetByUserIdAndYearMonth(target.getId(), yearMonth));
        return UserBudgetCompareResponse.of(
                BudgetCompareType.AMOUNT, yearMonth,
                me.getId(), "월 총 예산", myAmount,
                target.getId(), "월 총 예산", targetAmount);
    }

    private UserBudgetCompareResponse compareUsersByCategory(User me, User target, String yearMonth, Long categoryId) {
        if (categoryId == null) {
            throw new CustomException(BudgetErrorCode.CATEGORY_ID_REQUIRED);
        }
        BigDecimal myAmount = nullToZero(budgetRepository.findMyCategoryBudget(me.getId(), yearMonth, categoryId));
        BigDecimal targetAmount = nullToZero(budgetRepository.findMyCategoryBudget(target.getId(), yearMonth, categoryId));
        String categoryLabel = "카테고리 #" + categoryId;
        return UserBudgetCompareResponse.of(
                BudgetCompareType.CATEGORY, yearMonth,
                me.getId(), categoryLabel, myAmount,
                target.getId(), categoryLabel, targetAmount);
    }

    private UserBudgetDetailResponse toUserBudgetDetail(User user, String yearMonth) {
        List<Budget> budgets = budgetRepository.findByUserIdAndYearMonth(user.getId(), yearMonth);
        BigDecimal total = budgets.stream()
                .map(Budget::getTotalBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new UserBudgetDetailResponse(
                user.getId(), user.getUsername(), yearMonth, total,
                budgets.stream().map(CategoryBudgetResponse::from).toList());
    }

    private User loadPublicTarget(Long targetUserId) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        if (target.getUserSetting() == null || !Boolean.TRUE.equals(target.getUserSetting().getIsPortfolioPublic())) {
            throw new CustomException(BudgetErrorCode.TARGET_NOT_PUBLIC);
        }
        return target;
    }

    private String decadeLabel(LocalDate birthDate) {
        int age = LocalDate.now().getYear() - birthDate.getYear();
        return ((age / 10) * 10) + "대";
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    @Cacheable(
            cacheNames = RedisConfig.CACHE_COMPARE_BUDGET,
            key = "T(java.util.Objects).hash(#userId, #request.type(), #request.yearMonth(), #request.minAmount(), #request.maxAmount(), #request.categoryId(), #request.radiusKm())")
    public BudgetCompareResponse compareWithGroup(Long userId, BudgetCompareRequest request) {

        validateYearMonth(request.yearMonth());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        return switch (request.type()) {
            case AGE -> compareByAge(user, request.yearMonth());
            case AMOUNT -> compareByAmount(user, request.yearMonth(), request.minAmount(), request.maxAmount());
            case CATEGORY -> compareByCategory(user, request.yearMonth(), request.categoryId());
            case LOCATION -> compareByLocation(user, request.yearMonth(), request.radiusKm());
        };
    }

    private BudgetCompareResponse compareByLocation(User user, String yearMonth, Double radiusKm) {

        if (radiusKm == null || radiusKm <= 0) {
            throw new CustomException(BudgetErrorCode.RADIUS_REQUIRED);
        }

        Set<Long> nearbyUserIds = userLocationService.findNearbyUserIds(user.getId(), radiusKm);

        List<Object[]> rows = nearbyUserIds.isEmpty()
                ? List.of()
                : budgetRepository.sumMonthlyTotalsByUserIds(yearMonth, nearbyUserIds);

        BigDecimal myAmount = nullToZero(budgetRepository.sumTotalBudgetByUserIdAndYearMonth(user.getId(), yearMonth));
        return buildCompare(rows, myAmount, yearMonth, BudgetCompareType.LOCATION);
    }

    private BudgetCompareResponse compareByAge(User user, String yearMonth) {

        if (user.getBirthDate() == null) {
            throw new CustomException(BudgetErrorCode.BIRTH_DATE_REQUIRED);
        }

        int currentYear = LocalDate.now().getYear();
        int age = currentYear - user.getBirthDate().getYear();
        int decadeStart = (age / 10) * 10;

        LocalDate birthFrom = LocalDate.of(currentYear - (decadeStart + 9), 1, 1);
        LocalDate birthTo = LocalDate.of(currentYear - decadeStart, 12, 31);

        List<Object[]> rows = budgetRepository.sumMonthlyTotalsByAgeRange(
                yearMonth, birthFrom, birthTo, user.getId());

        BigDecimal myAmount = budgetRepository.sumTotalBudgetByUserIdAndYearMonth(user.getId(), yearMonth);
        return buildCompare(rows, myAmount, yearMonth, BudgetCompareType.AGE);
    }

    private BudgetCompareResponse compareByAmount(User user, String yearMonth, BigDecimal minAmount, BigDecimal maxAmount) {

        validateAmountRange(minAmount, maxAmount);

        List<Object[]> rows = budgetRepository.sumMonthlyTotalsByAmountRange(
                yearMonth, minAmount, maxAmount, user.getId());

        BigDecimal myAmount = budgetRepository.sumTotalBudgetByUserIdAndYearMonth(user.getId(), yearMonth);
        return buildCompare(rows, myAmount, yearMonth, BudgetCompareType.AMOUNT);
    }

    private BudgetCompareResponse compareByCategory(User user, String yearMonth, Long categoryId) {

        if (categoryId == null) {
            throw new CustomException(BudgetErrorCode.CATEGORY_ID_REQUIRED);
        }

        Object[] row = budgetRepository.averageCategoryBudget(yearMonth, categoryId, user.getId());

        BigDecimal average = BigDecimal.ZERO;
        long sampleSize = 0L;
        if (row != null && row.length >= 2 && row[1] != null) {
            sampleSize = ((Number) row[1]).longValue();
            if (sampleSize > 0 && row[0] != null) {
                average = toBigDecimal(row[0]).setScale(2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal myAmount = budgetRepository.findMyCategoryBudget(user.getId(), yearMonth, categoryId);
        if (myAmount == null) {
            myAmount = BigDecimal.ZERO;
        }

        return BudgetCompareResponse.of(
                BudgetCompareType.CATEGORY,
                yearMonth, myAmount, average, sampleSize);
    }

    private BudgetCompareResponse buildCompare(List<Object[]> rows, BigDecimal myAmount, String yearMonth,
                                               BudgetCompareType type) {
        long sampleSize = rows.size();
        BigDecimal average = BigDecimal.ZERO;
        if (sampleSize > 0) {
            BigDecimal sum = rows.stream()
                    .map(r -> toBigDecimal(r[1]))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            average = sum.divide(BigDecimal.valueOf(sampleSize), 2, RoundingMode.HALF_UP);
        }
        return BudgetCompareResponse.of(type, yearMonth, myAmount, average, sampleSize);
    }

    private void validateYearMonth(String yearMonth) {
        try {
            YearMonth.parse(yearMonth);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new CustomException(BudgetErrorCode.INVALID_YEAR_MONTH);
        }
    }

    private void validateAmountRange(BigDecimal minAmount, BigDecimal maxAmount) {
        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw new CustomException(BudgetErrorCode.INVALID_AMOUNT_RANGE);
        }
    }

    private String buildBoundary(Integer year, Integer month, boolean lower) {
        if (year == null && month == null) {
            return null;
        }
        if (year == null || month == null) {
            throw new CustomException(BudgetErrorCode.INVALID_YEAR_MONTH);
        }
        return String.format("%04d-%02d", year, month);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(value.toString());
    }
}
