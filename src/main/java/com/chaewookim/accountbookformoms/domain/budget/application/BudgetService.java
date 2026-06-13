package com.chaewookim.accountbookformoms.domain.budget.application;

import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionCategoryRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
import com.chaewookim.accountbookformoms.domain.budget.dao.BudgetRepository;
import com.chaewookim.accountbookformoms.domain.budget.dto.request.BudgetRequest;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.BudgetResponse;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.BudgetSummaryResponse;
import com.chaewookim.accountbookformoms.domain.budget.entity.Budget;
import com.chaewookim.accountbookformoms.domain.budget.error.BudgetErrorCode;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionCategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long createBudget(Long userId, BudgetRequest request) {

        if (budgetRepository.findByUserIdAndYearMonthAndTransactionCategoryId(userId, request.yearMonth(), request.categoryId()).isPresent()) {
            throw new CustomException(BudgetErrorCode.BUDGET_ALREADY_EXISTS);
        }

        Budget budget = Budget.builder()
                .user(userRepository.getReferenceById(userId))
                .transactionCategory(categoryRepository.getReferenceById(request.categoryId()))
                .yearMonth(request.yearMonth())
                .totalBudget(request.totalBudget())
                .expectedExpense(request.expectedExpense())
                .build();
        return budgetRepository.save(budget).getId();
    }

    public List<BudgetResponse> getMonthlyBudgetStatus(Long userId, String yearMonth) {

        List<Budget> budgets = budgetRepository.findByUserIdAndYearMonth(userId, yearMonth);
        List<Object[]> results = transactionRepository.sumAmountByUserIdGroupByCategoryId(userId, yearMonth);
        Map<Long, BigDecimal> expenseMap = results.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (BigDecimal) row[1]
                ));

        return budgets.stream().map(budget -> {

            Long categoryId = budget.getTransactionCategory().getId();

            BigDecimal totalPlannedBudget = budget.getTotalBudget().add(budget.getExpectedExpense());
            BigDecimal actualExpense = expenseMap.getOrDefault(categoryId, BigDecimal.ZERO);
            BigDecimal remaining = totalPlannedBudget.subtract(actualExpense);
            BigDecimal progress = calculateProgress(totalPlannedBudget, actualExpense);

            return new BudgetResponse(
                    budget.getId(),
                    categoryId,
                    budget.getTransactionCategory().getName(),
                    budget.getTotalBudget(),
                    budget.getExpectedExpense(),
                    totalPlannedBudget,
                    actualExpense,
                    remaining,
                    progress
            );
        }).toList();
    }

    public BudgetSummaryResponse getMonthlyBudgetSummary(Long userId, String yearMonth) {

        List<Budget> budgets = budgetRepository.findByUserIdAndYearMonth(userId, yearMonth);

        BigDecimal totalPlannedBudgetSum = budgets.stream()
                .map(budget -> budget.getTotalBudget().add(budget.getExpectedExpense()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate startDate = LocalDate.parse(yearMonth + "-01");
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        BigDecimal totalActualExpenseSum = transactionRepository.sumByUserAndType(userId, TransactionType.EXPENSE, startDate, endDate);
        if (totalActualExpenseSum == null) totalActualExpenseSum = BigDecimal.ZERO;

        BigDecimal totalRemainingBudget = totalPlannedBudgetSum.subtract(totalActualExpenseSum);

        return new BudgetSummaryResponse(yearMonth, totalPlannedBudgetSum, totalActualExpenseSum, totalRemainingBudget);
    }

    @Transactional
    public void updateBudget(Long userId, Long budgetId, BudgetRequest request) {
        Budget budget = validateAndGet(userId, budgetId);
        budget.update(request.totalBudget(), request.expectedExpense());

        User user = userRepository.findById(userId).orElseThrow();
        user.updateLastBudgetAlertMonth(null);
        userRepository.save(user);
    }

    @Transactional
    public void deleteBudget(Long userId, Long budgetId) {
        Budget budget = validateAndGet(userId, budgetId);
        budgetRepository.delete(budget);

        User user = userRepository.findById(userId).orElseThrow();
        user.updateLastBudgetAlertMonth(null);
        userRepository.save(user);
    }

    // 공통 검증 로직 분리
    private Budget validateAndGet(Long userId, Long budgetId) {

        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new CustomException(BudgetErrorCode.BUDGET_NOT_FOUND));

        if (!budget.getUser().getId().equals(userId)) {
            throw new CustomException(BudgetErrorCode.BUDGET_FORBIDDEN);
        }
        return budget;
    }

    // 예산 대비 실제 지출의 사용률 계산
    private BigDecimal calculateProgress(BigDecimal total, BigDecimal actual) {
        if (total.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return actual.divide(total, 2, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
    }

    // 카테고리별 예산 알림 생성 시 필요
    public BigDecimal getCategoryProgress(Long userId, String yearMonth, Long categoryId) {

        LocalDate startDate = LocalDate.parse(yearMonth + "-01");
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        return budgetRepository.findByUserIdAndYearMonthAndTransactionCategoryId(userId, yearMonth, categoryId)
                .map(budget -> {
                    BigDecimal actualExpense = transactionRepository.sumAmountByUserIdAndCategoryId(userId, categoryId, startDate, endDate);

                    if (actualExpense == null) actualExpense = BigDecimal.ZERO;

                    BigDecimal totalPlanned = budget.getTotalBudget().add(budget.getExpectedExpense());
                    return calculateProgress(totalPlanned, actualExpense);
                })
                .orElse(BigDecimal.ZERO);
    }
}
