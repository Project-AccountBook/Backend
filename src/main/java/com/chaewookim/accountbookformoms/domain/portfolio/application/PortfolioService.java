package com.chaewookim.accountbookformoms.domain.portfolio.application;

import com.chaewookim.accountbookformoms.domain.budget.application.BudgetCompareService;
import com.chaewookim.accountbookformoms.domain.budget.dto.request.BudgetCompareRequest;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.MyBudgetResponse;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.PairBudgetDetailResponse;
import com.chaewookim.accountbookformoms.domain.budget.enums.BudgetCompareType;
import com.chaewookim.accountbookformoms.domain.expense.application.ExpenseCompareService;
import com.chaewookim.accountbookformoms.domain.expense.dto.request.ExpenseCompareRequest;
import com.chaewookim.accountbookformoms.domain.expense.dto.response.MyExpenseResponse;
import com.chaewookim.accountbookformoms.domain.expense.dto.response.PairExpenseDetailResponse;
import com.chaewookim.accountbookformoms.domain.expense.enums.ExpenseCompareType;
import com.chaewookim.accountbookformoms.domain.income.application.IncomeCompareService;
import com.chaewookim.accountbookformoms.domain.income.dto.request.IncomeCompareRequest;
import com.chaewookim.accountbookformoms.domain.income.dto.response.MyIncomeResponse;
import com.chaewookim.accountbookformoms.domain.income.dto.response.PairIncomeDetailResponse;
import com.chaewookim.accountbookformoms.domain.income.enums.IncomeCompareType;
import com.chaewookim.accountbookformoms.domain.portfolio.dao.PortfolioAggregationRepository;
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
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioService {

    private final IncomeCompareService incomeCompareService;
    private final ExpenseCompareService expenseCompareService;
    private final BudgetCompareService budgetCompareService;
    private final PortfolioAggregationRepository portfolioAggregationRepository;

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

        YearMonth ym = YearMonth.of(filter.year(), filter.month());
        String yearMonth = ym.toString();
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        return portfolioAggregationRepository.findPublicPortfolios(
                        startDate, endDate, yearMonth,
                        filter.minIncome(), filter.maxIncome(),
                        filter.minExpense(), filter.maxExpense(),
                        filter.minBudget(), filter.maxBudget())
                .stream()
                .map(row -> PublicMonthlyPortfolioResponse.of(
                        row.userId(), row.username(), yearMonth,
                        row.totalIncome(), row.totalExpense(), row.totalBudget()))
                .toList();
    }

    public PortfolioCompareResponse compareWithGroup(Long userId, PortfolioCompareRequest request) {

        validateAmountRange(request.minIncome(), request.maxIncome());
        validateAmountRange(request.minExpense(), request.maxExpense());
        validateAmountRange(request.minBudget(), request.maxBudget());

        PortfolioCompareType type = request.type();
        String ym = request.yearMonth();

        IncomeCompareRequest incomeReq = new IncomeCompareRequest(
                toIncomeType(type), ym, request.minIncome(), request.maxIncome(), request.categoryId(), request.radiusKm());
        ExpenseCompareRequest expenseReq = new ExpenseCompareRequest(
                toExpenseType(type), ym, request.minExpense(), request.maxExpense(), request.categoryId(), request.radiusKm());
        BudgetCompareRequest budgetReq = new BudgetCompareRequest(
                toBudgetType(type), ym, request.minBudget(), request.maxBudget(), request.categoryId(), request.radiusKm());

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
            case LOCATION -> IncomeCompareType.LOCATION;
        };
    }

    private ExpenseCompareType toExpenseType(PortfolioCompareType type) {
        return switch (type) {
            case AGE -> ExpenseCompareType.AGE;
            case AMOUNT -> ExpenseCompareType.AMOUNT;
            case CATEGORY -> ExpenseCompareType.CATEGORY;
            case LOCATION -> ExpenseCompareType.LOCATION;
        };
    }

    private BudgetCompareType toBudgetType(PortfolioCompareType type) {
        return switch (type) {
            case AGE -> BudgetCompareType.AGE;
            case AMOUNT -> BudgetCompareType.AMOUNT;
            case CATEGORY -> BudgetCompareType.CATEGORY;
            case LOCATION -> BudgetCompareType.LOCATION;
        };
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
