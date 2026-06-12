package com.chaewookim.accountbookformoms.domain.expense.application;

import com.chaewookim.accountbookformoms.domain.asset.dao.FixedTransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
import com.chaewookim.accountbookformoms.domain.expense.dto.request.ExpenseCompareRequest;
import com.chaewookim.accountbookformoms.domain.expense.dto.request.PublicExpenseFilterRequest;
import com.chaewookim.accountbookformoms.domain.expense.dto.response.CategoryExpenseResponse;
import com.chaewookim.accountbookformoms.domain.expense.dto.response.ExpenseCompareResponse;
import com.chaewookim.accountbookformoms.domain.expense.dto.response.MyExpenseResponse;
import com.chaewookim.accountbookformoms.domain.expense.dto.response.PairExpenseDetailResponse;
import com.chaewookim.accountbookformoms.domain.expense.dto.response.PublicMonthlyExpenseResponse;
import com.chaewookim.accountbookformoms.domain.expense.dto.response.UserExpenseCompareResponse;
import com.chaewookim.accountbookformoms.domain.expense.dto.response.UserExpenseDetailResponse;
import com.chaewookim.accountbookformoms.domain.expense.enums.ExpenseCompareType;
import com.chaewookim.accountbookformoms.domain.expense.error.ExpenseErrorCode;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseCompareService {

    private static final TransactionType TYPE = TransactionType.EXPENSE;

    private final TransactionRepository transactionRepository;
    private final FixedTransactionRepository fixedTransactionRepository;
    private final UserRepository userRepository;

    public MyExpenseResponse getMyMonthlyExpense(Long userId, String yearMonth) {

        YearMonth ym = parseYearMonth(yearMonth);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        List<CategoryExpenseResponse> fixedCategories = toCategoryResponses(
                fixedTransactionRepository.sumByUserCategory(userId, TYPE, startDate, endDate));
        List<CategoryExpenseResponse> variableCategories = toCategoryResponses(
                transactionRepository.sumByUserCategory(userId, TYPE, startDate, endDate));

        BigDecimal fixedTotal = sumCategoryAmount(fixedCategories);
        BigDecimal variableTotal = sumCategoryAmount(variableCategories);

        return new MyExpenseResponse(
                yearMonth,
                fixedTotal.add(variableTotal),
                fixedTotal,
                variableTotal,
                fixedCategories,
                variableCategories);
    }

    public List<PublicMonthlyExpenseResponse> getPublicMonthlyExpenses(PublicExpenseFilterRequest filter) {

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
                    return new PublicMonthlyExpenseResponse(
                            userId, e.getValue(), yearMonth,
                            fixed.add(variable), fixed, variable);
                })
                .filter(r -> withinRange(r.totalExpense(), filter.minAmount(), filter.maxAmount()))
                .sorted((a, b) -> b.totalExpense().compareTo(a.totalExpense()))
                .toList();
    }

    public PairExpenseDetailResponse getPairExpenseDetails(Long myUserId, Long targetUserId, String yearMonth) {

        YearMonth ym = parseYearMonth(yearMonth);
        if (myUserId.equals(targetUserId)) {
            throw new CustomException(ExpenseErrorCode.CANNOT_COMPARE_SELF);
        }

        User me = userRepository.findById(myUserId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        User target = loadPublicTarget(targetUserId);

        return new PairExpenseDetailResponse(
                toUserExpenseDetail(me, ym, yearMonth),
                toUserExpenseDetail(target, ym, yearMonth));
    }

    public UserExpenseCompareResponse compareWithUser(Long myUserId, Long targetUserId,
                                                      ExpenseCompareType type, String yearMonth, Long categoryId) {

        YearMonth ym = parseYearMonth(yearMonth);
        if (myUserId.equals(targetUserId)) {
            throw new CustomException(ExpenseErrorCode.CANNOT_COMPARE_SELF);
        }

        User me = userRepository.findById(myUserId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        User target = loadPublicTarget(targetUserId);

        return switch (type) {
            case AGE -> compareUsersByAge(me, target, ym, yearMonth);
            case AMOUNT -> compareUsersByAmount(me, target, ym, yearMonth);
            case CATEGORY -> compareUsersByCategory(me, target, ym, yearMonth, categoryId);
        };
    }

    @Cacheable(
            cacheNames = RedisConfig.CACHE_COMPARE_EXPENSE,
            key = "T(java.util.Objects).hash(#userId, #request.type(), #request.yearMonth(), #request.minAmount(), #request.maxAmount(), #request.categoryId())")
    public ExpenseCompareResponse compareWithGroup(Long userId, ExpenseCompareRequest request) {

        YearMonth ym = parseYearMonth(request.yearMonth());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        return switch (request.type()) {
            case AGE -> compareByAge(user, ym, request.yearMonth());
            case AMOUNT -> compareByAmount(user, ym, request.yearMonth(), request.minAmount(), request.maxAmount());
            case CATEGORY -> compareByCategory(user, ym, request.yearMonth(), request.categoryId());
        };
    }

    private UserExpenseCompareResponse compareUsersByAge(User me, User target, YearMonth ym, String yearMonth) {
        if (me.getBirthDate() == null || target.getBirthDate() == null) {
            throw new CustomException(ExpenseErrorCode.BIRTH_DATE_REQUIRED);
        }
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();
        BigDecimal myFixed = nullToZero(fixedTransactionRepository.sumByUserAndType(me.getId(), TYPE, startDate, endDate));
        BigDecimal myVariable = nullToZero(transactionRepository.sumByUserAndType(me.getId(), TYPE, startDate, endDate));
        BigDecimal targetFixed = nullToZero(fixedTransactionRepository.sumByUserAndType(target.getId(), TYPE, startDate, endDate));
        BigDecimal targetVariable = nullToZero(transactionRepository.sumByUserAndType(target.getId(), TYPE, startDate, endDate));
        return UserExpenseCompareResponse.of(
                ExpenseCompareType.AGE, yearMonth,
                me.getId(), decadeLabel(me.getBirthDate()), myFixed, myVariable,
                target.getId(), decadeLabel(target.getBirthDate()), targetFixed, targetVariable);
    }

    private UserExpenseCompareResponse compareUsersByAmount(User me, User target, YearMonth ym, String yearMonth) {
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();
        BigDecimal myFixed = nullToZero(fixedTransactionRepository.sumByUserAndType(me.getId(), TYPE, startDate, endDate));
        BigDecimal myVariable = nullToZero(transactionRepository.sumByUserAndType(me.getId(), TYPE, startDate, endDate));
        BigDecimal targetFixed = nullToZero(fixedTransactionRepository.sumByUserAndType(target.getId(), TYPE, startDate, endDate));
        BigDecimal targetVariable = nullToZero(transactionRepository.sumByUserAndType(target.getId(), TYPE, startDate, endDate));
        return UserExpenseCompareResponse.of(
                ExpenseCompareType.AMOUNT, yearMonth,
                me.getId(), "월 총 지출", myFixed, myVariable,
                target.getId(), "월 총 지출", targetFixed, targetVariable);
    }

    private UserExpenseCompareResponse compareUsersByCategory(User me, User target, YearMonth ym,
                                                              String yearMonth, Long categoryId) {
        if (categoryId == null) {
            throw new CustomException(ExpenseErrorCode.CATEGORY_ID_REQUIRED);
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
        return UserExpenseCompareResponse.of(
                ExpenseCompareType.CATEGORY, yearMonth,
                me.getId(), categoryLabel, myFixed, myVariable,
                target.getId(), categoryLabel, targetFixed, targetVariable);
    }

    private ExpenseCompareResponse compareByAge(User user, YearMonth ym, String yearMonth) {

        if (user.getBirthDate() == null) {
            throw new CustomException(ExpenseErrorCode.BIRTH_DATE_REQUIRED);
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

        return buildGroupCompare(user, ym, yearMonth, fixedSums, variableSums, ExpenseCompareType.AGE);
    }

    private ExpenseCompareResponse compareByAmount(User user, YearMonth ym, String yearMonth,
                                                   BigDecimal minAmount, BigDecimal maxAmount) {

        validateAmountRange(minAmount, maxAmount);

        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        Map<Long, BigDecimal> fixedSums = toUserSumMap(fixedTransactionRepository.sumPublicByAmountRange(
                TYPE, startDate, endDate, minAmount, maxAmount, user.getId()));
        Map<Long, BigDecimal> variableSums = toUserSumMap(transactionRepository.sumPublicByAmountRange(
                TYPE, startDate, endDate, minAmount, maxAmount, user.getId()));

        return buildGroupCompare(user, ym, yearMonth, fixedSums, variableSums, ExpenseCompareType.AMOUNT);
    }

    private ExpenseCompareResponse compareByCategory(User user, YearMonth ym, String yearMonth, Long categoryId) {

        if (categoryId == null) {
            throw new CustomException(ExpenseErrorCode.CATEGORY_ID_REQUIRED);
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

        return buildAverage(yearMonth, ExpenseCompareType.CATEGORY,
                myFixed, myVariable, fixedSums, variableSums);
    }

    private ExpenseCompareResponse buildGroupCompare(User user, YearMonth ym, String yearMonth,
                                                     Map<Long, BigDecimal> fixedSums,
                                                     Map<Long, BigDecimal> variableSums,
                                                     ExpenseCompareType type) {
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();
        BigDecimal myFixed = nullToZero(fixedTransactionRepository.sumByUserAndType(user.getId(), TYPE, startDate, endDate));
        BigDecimal myVariable = nullToZero(transactionRepository.sumByUserAndType(user.getId(), TYPE, startDate, endDate));
        return buildAverage(yearMonth, type, myFixed, myVariable, fixedSums, variableSums);
    }

    private ExpenseCompareResponse buildAverage(String yearMonth, ExpenseCompareType type,
                                                BigDecimal myFixed, BigDecimal myVariable,
                                                Map<Long, BigDecimal> fixedSums,
                                                Map<Long, BigDecimal> variableSums) {
        Map<Long, BigDecimal> merged = new HashMap<>(fixedSums);
        variableSums.forEach((k, v) -> merged.merge(k, v, BigDecimal::add));
        long sampleSize = merged.size();

        BigDecimal averageFixed = average(fixedSums, sampleSize);
        BigDecimal averageVariable = average(variableSums, sampleSize);

        return ExpenseCompareResponse.of(type, yearMonth, myFixed, myVariable, averageFixed, averageVariable, sampleSize);
    }

    private BigDecimal average(Map<Long, BigDecimal> sums, long sampleSize) {
        if (sampleSize <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = sums.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(sampleSize), 2, RoundingMode.HALF_UP);
    }

    private UserExpenseDetailResponse toUserExpenseDetail(User user, YearMonth ym, String yearMonth) {
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();
        List<CategoryExpenseResponse> fixedCategories = toCategoryResponses(
                fixedTransactionRepository.sumByUserCategory(user.getId(), TYPE, startDate, endDate));
        List<CategoryExpenseResponse> variableCategories = toCategoryResponses(
                transactionRepository.sumByUserCategory(user.getId(), TYPE, startDate, endDate));
        BigDecimal fixedTotal = sumCategoryAmount(fixedCategories);
        BigDecimal variableTotal = sumCategoryAmount(variableCategories);
        return new UserExpenseDetailResponse(
                user.getId(), user.getUsername(), yearMonth,
                fixedTotal.add(variableTotal), fixedTotal, variableTotal,
                fixedCategories, variableCategories);
    }

    private User loadPublicTarget(Long targetUserId) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        if (target.getUserSetting() == null || !Boolean.TRUE.equals(target.getUserSetting().getIsPortfolioPublic())) {
            throw new CustomException(ExpenseErrorCode.TARGET_NOT_PUBLIC);
        }
        return target;
    }

    private List<CategoryExpenseResponse> toCategoryResponses(List<Object[]> rows) {
        return rows.stream()
                .map(r -> new CategoryExpenseResponse(
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

    private BigDecimal sumCategoryAmount(List<CategoryExpenseResponse> categories) {
        return categories.stream()
                .map(CategoryExpenseResponse::amount)
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
            throw new CustomException(ExpenseErrorCode.INVALID_YEAR_MONTH);
        }
    }

    private YearMonth buildYearMonth(Integer year, Integer month) {
        if (year == null || month == null) {
            throw new CustomException(ExpenseErrorCode.INVALID_YEAR_MONTH);
        }
        try {
            return YearMonth.of(year, month);
        } catch (Exception e) {
            throw new CustomException(ExpenseErrorCode.INVALID_YEAR_MONTH);
        }
    }

    private void validateAmountRange(BigDecimal minAmount, BigDecimal maxAmount) {
        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw new CustomException(ExpenseErrorCode.INVALID_AMOUNT_RANGE);
        }
    }
}
