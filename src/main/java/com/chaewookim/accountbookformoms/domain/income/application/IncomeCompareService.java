package com.chaewookim.accountbookformoms.domain.income.application;

import com.chaewookim.accountbookformoms.domain.asset.dao.FixedTransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
import com.chaewookim.accountbookformoms.domain.income.dto.request.IncomeCompareRequest;
import com.chaewookim.accountbookformoms.domain.income.dto.request.PublicIncomeFilterRequest;
import com.chaewookim.accountbookformoms.domain.income.dto.response.CategoryIncomeResponse;
import com.chaewookim.accountbookformoms.domain.income.dto.response.IncomeCompareResponse;
import com.chaewookim.accountbookformoms.domain.income.dto.response.MyIncomeResponse;
import com.chaewookim.accountbookformoms.domain.income.dto.response.PairIncomeDetailResponse;
import com.chaewookim.accountbookformoms.domain.income.dto.response.PublicMonthlyIncomeResponse;
import com.chaewookim.accountbookformoms.domain.income.dto.response.UserIncomeCompareResponse;
import com.chaewookim.accountbookformoms.domain.income.dto.response.UserIncomeDetailResponse;
import com.chaewookim.accountbookformoms.domain.income.enums.IncomeCompareType;
import com.chaewookim.accountbookformoms.domain.income.error.IncomeErrorCode;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncomeCompareService {

    private static final TransactionType TYPE = TransactionType.INCOME;
    private static final double MIN_RADIUS_KM = 0.1;
    private static final double MAX_RADIUS_KM = 50.0;

    private final TransactionRepository transactionRepository;
    private final FixedTransactionRepository fixedTransactionRepository;
    private final UserRepository userRepository;
    private final UserLocationService userLocationService;

    public MyIncomeResponse getMyMonthlyIncome(Long userId, String yearMonth) {

        YearMonth ym = parseYearMonth(yearMonth);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        List<CategoryIncomeResponse> fixedCategories = toCategoryResponses(
                fixedTransactionRepository.sumByUserCategory(userId, TYPE, startDate, endDate));
        List<CategoryIncomeResponse> variableCategories = toCategoryResponses(
                transactionRepository.sumByUserCategory(userId, TYPE, startDate, endDate));

        BigDecimal fixedTotal = sumCategoryAmount(fixedCategories);
        BigDecimal variableTotal = sumCategoryAmount(variableCategories);

        return new MyIncomeResponse(
                yearMonth,
                fixedTotal.add(variableTotal),
                fixedTotal,
                variableTotal,
                fixedCategories,
                variableCategories);
    }

    public List<PublicMonthlyIncomeResponse> getPublicMonthlyIncomes(PublicIncomeFilterRequest filter) {

        validateAmountRange(filter.minAmount(), filter.maxAmount());

        YearMonth ym = buildYearMonth(filter.year(), filter.month());
        String yearMonth = ym.toString();
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        List<Object[]> fixedRows = fixedTransactionRepository.sumPublicMonthly(TYPE, startDate, endDate);
        List<Object[]> variableRows = transactionRepository.sumPublicMonthly(TYPE, startDate, endDate);

        Map<Long, BigDecimal> fixedSums = toUserSumMap(fixedRows);
        Map<Long, BigDecimal> variableSums = toUserSumMap(variableRows);
        Map<Long, String> usernames = collectUsernames(fixedRows, variableRows);

        return usernames.entrySet().stream()
                .map(e -> {
                    Long userId = e.getKey();
                    BigDecimal fixed = fixedSums.getOrDefault(userId, BigDecimal.ZERO);
                    BigDecimal variable = variableSums.getOrDefault(userId, BigDecimal.ZERO);
                    return new PublicMonthlyIncomeResponse(
                            userId, e.getValue(), yearMonth,
                            fixed.add(variable), fixed, variable);
                })
                .filter(r -> withinRange(r.totalIncome(), filter.minAmount(), filter.maxAmount()))
                .sorted((a, b) -> b.totalIncome().compareTo(a.totalIncome()))
                .toList();
    }

    public PairIncomeDetailResponse getPairIncomeDetails(Long myUserId, Long targetUserId, String yearMonth) {

        YearMonth ym = parseYearMonth(yearMonth);
        if (myUserId.equals(targetUserId)) {
            throw new CustomException(IncomeErrorCode.CANNOT_COMPARE_SELF);
        }

        User me = userRepository.findById(myUserId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        User target = loadPublicTarget(targetUserId);

        return new PairIncomeDetailResponse(
                toUserIncomeDetail(me, ym, yearMonth),
                toUserIncomeDetail(target, ym, yearMonth));
    }

    public UserIncomeCompareResponse compareWithUser(Long myUserId, Long targetUserId,
                                                     IncomeCompareType type, String yearMonth, Long categoryId) {

        YearMonth ym = parseYearMonth(yearMonth);
        if (myUserId.equals(targetUserId)) {
            throw new CustomException(IncomeErrorCode.CANNOT_COMPARE_SELF);
        }

        User me = userRepository.findById(myUserId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        User target = loadPublicTarget(targetUserId);

        return switch (type) {
            case AGE -> compareUsersByAge(me, target, ym, yearMonth);
            case AMOUNT -> compareUsersByAmount(me, target, ym, yearMonth);
            case CATEGORY -> compareUsersByCategory(me, target, ym, yearMonth, categoryId);
            case LOCATION -> throw new CustomException(IncomeErrorCode.LOCATION_PAIR_NOT_SUPPORTED);
        };
    }

    @Cacheable(
            cacheNames = RedisConfig.CACHE_COMPARE_INCOME,
            key = "T(java.util.Objects).hash(#userId, #request.type(), #request.yearMonth(), #request.minAmount(), #request.maxAmount(), #request.categoryId(), #request.radiusKm())")
    public IncomeCompareResponse compareWithGroup(Long userId, IncomeCompareRequest request) {

        YearMonth ym = parseYearMonth(request.yearMonth());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        return switch (request.type()) {
            case AGE -> compareByAge(user, ym, request.yearMonth());
            case AMOUNT -> compareByAmount(user, ym, request.yearMonth(), request.minAmount(), request.maxAmount());
            case CATEGORY -> compareByCategory(user, ym, request.yearMonth(), request.categoryId());
            case LOCATION -> compareByLocation(user, ym, request.yearMonth(), request.radiusKm());
        };
    }

    private IncomeCompareResponse compareByLocation(User user, YearMonth ym, String yearMonth, Double radiusKm) {

        if (radiusKm == null) {
            throw new CustomException(IncomeErrorCode.RADIUS_REQUIRED);
        }
        if (radiusKm < MIN_RADIUS_KM || radiusKm > MAX_RADIUS_KM) {
            throw new CustomException(IncomeErrorCode.INVALID_RADIUS);
        }

        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        Set<Long> nearbyUserIds = userLocationService.findNearbyUserIds(user.getId(), radiusKm);

        Map<Long, BigDecimal> fixedSums = nearbyUserIds.isEmpty()
                ? Map.of()
                : toUserSumMap(fixedTransactionRepository.sumPublicByUserIds(TYPE, startDate, endDate, nearbyUserIds));
        Map<Long, BigDecimal> variableSums = nearbyUserIds.isEmpty()
                ? Map.of()
                : toUserSumMap(transactionRepository.sumPublicByUserIds(TYPE, startDate, endDate, nearbyUserIds));

        return buildGroupCompare(user, ym, yearMonth, fixedSums, variableSums, IncomeCompareType.LOCATION);
    }

    private UserIncomeCompareResponse compareUsersByAge(User me, User target, YearMonth ym, String yearMonth) {
        if (me.getBirthDate() == null || target.getBirthDate() == null) {
            throw new CustomException(IncomeErrorCode.BIRTH_DATE_REQUIRED);
        }
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();
        BigDecimal myFixed = nullToZero(fixedTransactionRepository.sumByUserAndType(me.getId(), TYPE, startDate, endDate));
        BigDecimal myVariable = nullToZero(transactionRepository.sumByUserAndType(me.getId(), TYPE, startDate, endDate));
        BigDecimal targetFixed = nullToZero(fixedTransactionRepository.sumByUserAndType(target.getId(), TYPE, startDate, endDate));
        BigDecimal targetVariable = nullToZero(transactionRepository.sumByUserAndType(target.getId(), TYPE, startDate, endDate));
        return UserIncomeCompareResponse.of(
                IncomeCompareType.AGE, yearMonth,
                me.getId(), decadeLabel(me.getBirthDate()), myFixed, myVariable,
                target.getId(), decadeLabel(target.getBirthDate()), targetFixed, targetVariable);
    }

    private UserIncomeCompareResponse compareUsersByAmount(User me, User target, YearMonth ym, String yearMonth) {
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();
        BigDecimal myFixed = nullToZero(fixedTransactionRepository.sumByUserAndType(me.getId(), TYPE, startDate, endDate));
        BigDecimal myVariable = nullToZero(transactionRepository.sumByUserAndType(me.getId(), TYPE, startDate, endDate));
        BigDecimal targetFixed = nullToZero(fixedTransactionRepository.sumByUserAndType(target.getId(), TYPE, startDate, endDate));
        BigDecimal targetVariable = nullToZero(transactionRepository.sumByUserAndType(target.getId(), TYPE, startDate, endDate));
        return UserIncomeCompareResponse.of(
                IncomeCompareType.AMOUNT, yearMonth,
                me.getId(), "월 총 수입", myFixed, myVariable,
                target.getId(), "월 총 수입", targetFixed, targetVariable);
    }

    private UserIncomeCompareResponse compareUsersByCategory(User me, User target, YearMonth ym,
                                                             String yearMonth, Long categoryId) {
        if (categoryId == null) {
            throw new CustomException(IncomeErrorCode.CATEGORY_ID_REQUIRED);
        }
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();
        BigDecimal myFixed = nullToZero(fixedTransactionRepository.sumByUserAndTypeAndCategory(
                me.getId(), TYPE, categoryId, startDate, endDate));
        BigDecimal myVariable = nullToZero(transactionRepository.sumByUserAndTypeAndCategory(
                me.getId(), TYPE, categoryId, startDate, endDate));
        BigDecimal targetFixed = nullToZero(fixedTransactionRepository.sumByUserAndTypeAndCategory(
                target.getId(), TYPE, categoryId, startDate, endDate));
        BigDecimal targetVariable = nullToZero(transactionRepository.sumByUserAndTypeAndCategory(
                target.getId(), TYPE, categoryId, startDate, endDate));
        String categoryLabel = "카테고리 #" + categoryId;
        return UserIncomeCompareResponse.of(
                IncomeCompareType.CATEGORY, yearMonth,
                me.getId(), categoryLabel, myFixed, myVariable,
                target.getId(), categoryLabel, targetFixed, targetVariable);
    }

    private IncomeCompareResponse compareByAge(User user, YearMonth ym, String yearMonth) {

        if (user.getBirthDate() == null) {
            throw new CustomException(IncomeErrorCode.BIRTH_DATE_REQUIRED);
        }

        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        int currentYear = LocalDate.now().getYear();
        int age = currentYear - user.getBirthDate().getYear();
        int decadeStart = (age / 10) * 10;
        LocalDate birthFrom = LocalDate.of(currentYear - (decadeStart + 9), 1, 1);
        LocalDate birthTo = LocalDate.of(currentYear - decadeStart, 12, 31);

        Map<Long, BigDecimal> fixedSums = toUserSumMap(fixedTransactionRepository.sumPublicByAgeRange(
                TYPE, startDate, endDate, birthFrom, birthTo, user.getId()));
        Map<Long, BigDecimal> variableSums = toUserSumMap(transactionRepository.sumPublicByAgeRange(
                TYPE, startDate, endDate, birthFrom, birthTo, user.getId()));

        return buildGroupCompare(user, ym, yearMonth, fixedSums, variableSums, IncomeCompareType.AGE);
    }

    private IncomeCompareResponse compareByAmount(User user, YearMonth ym, String yearMonth,
                                                  BigDecimal minAmount, BigDecimal maxAmount) {

        validateAmountRange(minAmount, maxAmount);

        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        Map<Long, BigDecimal> fixedSums = toUserSumMap(fixedTransactionRepository.sumPublicByAmountRange(
                TYPE, startDate, endDate, minAmount, maxAmount, user.getId()));
        Map<Long, BigDecimal> variableSums = toUserSumMap(transactionRepository.sumPublicByAmountRange(
                TYPE, startDate, endDate, minAmount, maxAmount, user.getId()));

        return buildGroupCompare(user, ym, yearMonth, fixedSums, variableSums, IncomeCompareType.AMOUNT);
    }

    private IncomeCompareResponse compareByCategory(User user, YearMonth ym, String yearMonth, Long categoryId) {

        if (categoryId == null) {
            throw new CustomException(IncomeErrorCode.CATEGORY_ID_REQUIRED);
        }

        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        Map<Long, BigDecimal> fixedSums = toUserSumMap(fixedTransactionRepository.sumPublicCategoryByUser(
                TYPE, categoryId, startDate, endDate, user.getId()));
        Map<Long, BigDecimal> variableSums = toUserSumMap(transactionRepository.sumPublicCategoryByUser(
                TYPE, categoryId, startDate, endDate, user.getId()));

        BigDecimal myFixed = nullToZero(fixedTransactionRepository.sumByUserAndTypeAndCategory(
                user.getId(), TYPE, categoryId, startDate, endDate));
        BigDecimal myVariable = nullToZero(transactionRepository.sumByUserAndTypeAndCategory(
                user.getId(), TYPE, categoryId, startDate, endDate));

        return buildAverage(yearMonth, IncomeCompareType.CATEGORY,
                myFixed, myVariable, fixedSums, variableSums);
    }

    private IncomeCompareResponse buildGroupCompare(User user, YearMonth ym, String yearMonth,
                                                    Map<Long, BigDecimal> fixedSums,
                                                    Map<Long, BigDecimal> variableSums,
                                                    IncomeCompareType type) {
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();
        BigDecimal myFixed = nullToZero(fixedTransactionRepository.sumByUserAndType(user.getId(), TYPE, startDate, endDate));
        BigDecimal myVariable = nullToZero(transactionRepository.sumByUserAndType(user.getId(), TYPE, startDate, endDate));
        return buildAverage(yearMonth, type, myFixed, myVariable, fixedSums, variableSums);
    }

    private IncomeCompareResponse buildAverage(String yearMonth, IncomeCompareType type,
                                               BigDecimal myFixed, BigDecimal myVariable,
                                               Map<Long, BigDecimal> fixedSums,
                                               Map<Long, BigDecimal> variableSums) {
        Map<Long, BigDecimal> merged = new HashMap<>(fixedSums);
        variableSums.forEach((k, v) -> merged.merge(k, v, BigDecimal::add));
        long sampleSize = merged.size();

        BigDecimal averageFixed = average(fixedSums, sampleSize);
        BigDecimal averageVariable = average(variableSums, sampleSize);

        return IncomeCompareResponse.of(type, yearMonth, myFixed, myVariable, averageFixed, averageVariable, sampleSize);
    }

    private BigDecimal average(Map<Long, BigDecimal> sums, long sampleSize) {
        if (sampleSize <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = sums.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(sampleSize), 2, RoundingMode.HALF_UP);
    }

    private UserIncomeDetailResponse toUserIncomeDetail(User user, YearMonth ym, String yearMonth) {
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();
        List<CategoryIncomeResponse> fixedCategories = toCategoryResponses(
                fixedTransactionRepository.sumByUserCategory(user.getId(), TYPE, startDate, endDate));
        List<CategoryIncomeResponse> variableCategories = toCategoryResponses(
                transactionRepository.sumByUserCategory(user.getId(), TYPE, startDate, endDate));
        BigDecimal fixedTotal = sumCategoryAmount(fixedCategories);
        BigDecimal variableTotal = sumCategoryAmount(variableCategories);
        return new UserIncomeDetailResponse(
                user.getId(), user.getUsername(), yearMonth,
                fixedTotal.add(variableTotal), fixedTotal, variableTotal,
                fixedCategories, variableCategories);
    }

    private User loadPublicTarget(Long targetUserId) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        if (target.getUserSetting() == null || !Boolean.TRUE.equals(target.getUserSetting().getIsPortfolioPublic())) {
            throw new CustomException(IncomeErrorCode.TARGET_NOT_PUBLIC);
        }
        return target;
    }

    private List<CategoryIncomeResponse> toCategoryResponses(List<Object[]> rows) {
        return rows.stream()
                .map(r -> new CategoryIncomeResponse(
                        (Long) r[0],
                        (String) r[1],
                        toBigDecimal(r[2])))
                .toList();
    }

    private Map<Long, BigDecimal> toUserSumMap(List<Object[]> rows) {
        Map<Long, BigDecimal> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], toBigDecimal(row[row.length - 1]));
        }
        return map;
    }

    private Map<Long, String> collectUsernames(List<Object[]> rowsA, List<Object[]> rowsB) {
        Map<Long, String> map = new HashMap<>();
        for (Object[] row : rowsA) {
            map.putIfAbsent((Long) row[0], (String) row[1]);
        }
        for (Object[] row : rowsB) {
            map.putIfAbsent((Long) row[0], (String) row[1]);
        }
        return map;
    }

    private BigDecimal sumCategoryAmount(List<CategoryIncomeResponse> categories) {
        return categories.stream()
                .map(CategoryIncomeResponse::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean withinRange(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (min != null && value.compareTo(min) < 0) return false;
        if (max != null && value.compareTo(max) > 0) return false;
        return true;
    }

    private String decadeLabel(LocalDate birthDate) {
        int age = LocalDate.now().getYear() - birthDate.getYear();
        return ((age / 10) * 10) + "대";
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }

    private YearMonth parseYearMonth(String yearMonth) {
        try {
            return YearMonth.parse(yearMonth);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new CustomException(IncomeErrorCode.INVALID_YEAR_MONTH);
        }
    }

    private YearMonth buildYearMonth(Integer year, Integer month) {
        if (year == null || month == null) {
            throw new CustomException(IncomeErrorCode.INVALID_YEAR_MONTH);
        }
        try {
            return YearMonth.of(year, month);
        } catch (Exception e) {
            throw new CustomException(IncomeErrorCode.INVALID_YEAR_MONTH);
        }
    }

    private void validateAmountRange(BigDecimal minAmount, BigDecimal maxAmount) {
        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw new CustomException(IncomeErrorCode.INVALID_AMOUNT_RANGE);
        }
    }
}
