package com.chaewookim.accountbookformoms.domain.portfolio.application;

import com.chaewookim.accountbookformoms.domain.budget.application.BudgetCompareService;
import com.chaewookim.accountbookformoms.domain.budget.dto.request.BudgetCompareRequest;
import com.chaewookim.accountbookformoms.domain.budget.dto.request.PublicBudgetFilterRequest;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.MyBudgetResponse;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.PairBudgetDetailResponse;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.PublicMonthlyBudgetResponse;
import com.chaewookim.accountbookformoms.domain.budget.enums.BudgetCompareType;
import com.chaewookim.accountbookformoms.domain.expense.application.ExpenseCompareService;
import com.chaewookim.accountbookformoms.domain.expense.dto.request.ExpenseCompareRequest;
import com.chaewookim.accountbookformoms.domain.expense.dto.request.PublicExpenseFilterRequest;
import com.chaewookim.accountbookformoms.domain.expense.dto.response.MyExpenseResponse;
import com.chaewookim.accountbookformoms.domain.expense.dto.response.PairExpenseDetailResponse;
import com.chaewookim.accountbookformoms.domain.expense.dto.response.PublicMonthlyExpenseResponse;
import com.chaewookim.accountbookformoms.domain.expense.enums.ExpenseCompareType;
import com.chaewookim.accountbookformoms.domain.income.application.IncomeCompareService;
import com.chaewookim.accountbookformoms.domain.income.dto.request.IncomeCompareRequest;
import com.chaewookim.accountbookformoms.domain.income.dto.request.PublicIncomeFilterRequest;
import com.chaewookim.accountbookformoms.domain.income.dto.response.MyIncomeResponse;
import com.chaewookim.accountbookformoms.domain.income.dto.response.PairIncomeDetailResponse;
import com.chaewookim.accountbookformoms.domain.income.dto.response.PublicMonthlyIncomeResponse;
import com.chaewookim.accountbookformoms.domain.income.enums.IncomeCompareType;
import com.chaewookim.accountbookformoms.domain.portfolio.dto.request.PortfolioCompareRequest;
import com.chaewookim.accountbookformoms.domain.portfolio.dto.request.PortfolioFilterRequest;
import com.chaewookim.accountbookformoms.domain.portfolio.dto.response.MyPortfolioResponse;
import com.chaewookim.accountbookformoms.domain.portfolio.dto.response.PairPortfolioDetailResponse;
import com.chaewookim.accountbookformoms.domain.portfolio.dto.response.PortfolioCompareResponse;
import com.chaewookim.accountbookformoms.domain.portfolio.dto.response.PublicMonthlyPortfolioResponse;
import com.chaewookim.accountbookformoms.domain.portfolio.dto.response.UserPortfolioCompareResponse;
import com.chaewookim.accountbookformoms.domain.portfolio.dto.response.UserPortfolioDetailResponse;
import com.chaewookim.accountbookformoms.domain.portfolio.enums.PortfolioCompareType;
import com.chaewookim.accountbookformoms.domain.portfolio.error.PortfolioErrorCode;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioService {

    private final IncomeCompareService incomeCompareService;
    private final ExpenseCompareService expenseCompareService;
    private final BudgetCompareService budgetCompareService;

    public MyPortfolioResponse getMyPortfolio(Long userId, String yearMonth) {

        validateYearMonth(yearMonth);

        MyIncomeResponse income = incomeCompareService.getMyMonthlyIncome(userId, yearMonth);
        MyExpenseResponse expense = expenseCompareService.getMyMonthlyExpense(userId, yearMonth);
        MyBudgetResponse budget = budgetCompareService.getMyMonthlyBudget(userId, yearMonth);

        return MyPortfolioResponse.of(yearMonth, income, expense, budget);
    }

    public List<PublicMonthlyPortfolioResponse> getPublicPortfolios(PortfolioFilterRequest filter) {

        validateAmountRange(filter.minIncome(), filter.maxIncome());
        validateAmountRange(filter.minExpense(), filter.maxExpense());
        validateAmountRange(filter.minBudget(), filter.maxBudget());

        if (filter.year() == null || filter.month() == null) {
            throw new CustomException(PortfolioErrorCode.INVALID_YEAR_MONTH);
        }

        int year = filter.year();
        int month = filter.month();
        String yearMonth = String.format("%04d-%02d", year, month);

        List<PublicMonthlyIncomeResponse> incomes = incomeCompareService.getPublicMonthlyIncomes(
                new PublicIncomeFilterRequest(year, month, null, null));
        List<PublicMonthlyExpenseResponse> expenses = expenseCompareService.getPublicMonthlyExpenses(
                new PublicExpenseFilterRequest(year, month, null, null));
        List<PublicMonthlyBudgetResponse> budgets = budgetCompareService.getPublicMonthlyBudgets(
                new PublicBudgetFilterRequest(year, year, month, month, null, null));

        Map<Long, BigDecimal> incomeMap = new HashMap<>();
        Map<Long, BigDecimal> expenseMap = new HashMap<>();
        Map<Long, BigDecimal> budgetMap = new HashMap<>();
        Map<Long, String> usernames = new HashMap<>();

        for (PublicMonthlyIncomeResponse r : incomes) {
            incomeMap.put(r.userId(), r.totalIncome());
            usernames.putIfAbsent(r.userId(), r.username());
        }
        for (PublicMonthlyExpenseResponse r : expenses) {
            expenseMap.put(r.userId(), r.totalExpense());
            usernames.putIfAbsent(r.userId(), r.username());
        }
        for (PublicMonthlyBudgetResponse r : budgets) {
            budgetMap.put(r.userId(), r.totalBudget());
            usernames.putIfAbsent(r.userId(), r.username());
        }

        Set<Long> userIds = new LinkedHashSet<>();
        userIds.addAll(incomeMap.keySet());
        userIds.addAll(expenseMap.keySet());
        userIds.addAll(budgetMap.keySet());

        return userIds.stream()
                .map(uid -> PublicMonthlyPortfolioResponse.of(
                        uid,
                        usernames.get(uid),
                        yearMonth,
                        incomeMap.getOrDefault(uid, BigDecimal.ZERO),
                        expenseMap.getOrDefault(uid, BigDecimal.ZERO),
                        budgetMap.getOrDefault(uid, BigDecimal.ZERO)))
                .filter(r -> withinRange(r.totalIncome(), filter.minIncome(), filter.maxIncome()))
                .filter(r -> withinRange(r.totalExpense(), filter.minExpense(), filter.maxExpense()))
                .filter(r -> withinRange(r.totalBudget(), filter.minBudget(), filter.maxBudget()))
                .sorted((a, b) -> b.balance().compareTo(a.balance()))
                .toList();
    }

    public PortfolioCompareResponse compareWithGroup(Long userId, PortfolioCompareRequest request) {

        validateAmountRange(request.minIncome(), request.maxIncome());
        validateAmountRange(request.minExpense(), request.maxExpense());
        validateAmountRange(request.minBudget(), request.maxBudget());

        PortfolioCompareType type = request.type();
        String ym = request.yearMonth();

        IncomeCompareRequest incomeReq = new IncomeCompareRequest(
                toIncomeType(type), ym, request.minIncome(), request.maxIncome(), request.categoryId());
        ExpenseCompareRequest expenseReq = new ExpenseCompareRequest(
                toExpenseType(type), ym, request.minExpense(), request.maxExpense(), request.categoryId());
        BudgetCompareRequest budgetReq = new BudgetCompareRequest(
                toBudgetType(type), ym, request.minBudget(), request.maxBudget(), request.categoryId());

        return new PortfolioCompareResponse(
                type, ym,
                incomeCompareService.compareWithGroup(userId, incomeReq),
                expenseCompareService.compareWithGroup(userId, expenseReq),
                budgetCompareService.compareWithGroup(userId, budgetReq));
    }

    public PairPortfolioDetailResponse getPairPortfolioDetails(Long myUserId, Long targetUserId, String yearMonth) {

        validateYearMonth(yearMonth);
        if (myUserId.equals(targetUserId)) {
            throw new CustomException(PortfolioErrorCode.CANNOT_COMPARE_SELF);
        }

        PairIncomeDetailResponse income = incomeCompareService.getPairIncomeDetails(myUserId, targetUserId, yearMonth);
        PairExpenseDetailResponse expense = expenseCompareService.getPairExpenseDetails(myUserId, targetUserId, yearMonth);
        PairBudgetDetailResponse budget = budgetCompareService.getPairBudgetDetails(myUserId, targetUserId, yearMonth);

        return new PairPortfolioDetailResponse(
                UserPortfolioDetailResponse.of(income.me(), expense.me(), budget.me()),
                UserPortfolioDetailResponse.of(income.target(), expense.target(), budget.target()));
    }

    public UserPortfolioCompareResponse compareWithUser(Long myUserId, Long targetUserId,
                                                       PortfolioCompareType type, String yearMonth, Long categoryId) {

        validateYearMonth(yearMonth);
        if (myUserId.equals(targetUserId)) {
            throw new CustomException(PortfolioErrorCode.CANNOT_COMPARE_SELF);
        }

        return new UserPortfolioCompareResponse(
                type, yearMonth, myUserId, targetUserId,
                incomeCompareService.compareWithUser(myUserId, targetUserId, toIncomeType(type), yearMonth, categoryId),
                expenseCompareService.compareWithUser(myUserId, targetUserId, toExpenseType(type), yearMonth, categoryId),
                budgetCompareService.compareWithUser(myUserId, targetUserId, toBudgetType(type), yearMonth, categoryId));
    }

    private IncomeCompareType toIncomeType(PortfolioCompareType type) {
        return switch (type) {
            case AGE -> IncomeCompareType.AGE;
            case AMOUNT -> IncomeCompareType.AMOUNT;
            case CATEGORY -> IncomeCompareType.CATEGORY;
        };
    }

    private ExpenseCompareType toExpenseType(PortfolioCompareType type) {
        return switch (type) {
            case AGE -> ExpenseCompareType.AGE;
            case AMOUNT -> ExpenseCompareType.AMOUNT;
            case CATEGORY -> ExpenseCompareType.CATEGORY;
        };
    }

    private BudgetCompareType toBudgetType(PortfolioCompareType type) {
        return switch (type) {
            case AGE -> BudgetCompareType.AGE;
            case AMOUNT -> BudgetCompareType.AMOUNT;
            case CATEGORY -> BudgetCompareType.CATEGORY;
        };
    }

    private boolean withinRange(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (min != null && value.compareTo(min) < 0) return false;
        if (max != null && value.compareTo(max) > 0) return false;
        return true;
    }

    private void validateYearMonth(String yearMonth) {
        try {
            YearMonth.parse(yearMonth);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new CustomException(PortfolioErrorCode.INVALID_YEAR_MONTH);
        }
    }

    private void validateAmountRange(BigDecimal minAmount, BigDecimal maxAmount) {
        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw new CustomException(PortfolioErrorCode.INVALID_AMOUNT_RANGE);
        }
    }
}
