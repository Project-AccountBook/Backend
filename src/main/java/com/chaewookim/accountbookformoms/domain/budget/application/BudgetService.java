package com.chaewookim.accountbookformoms.domain.budget.application;

import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionCategoryRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
import com.chaewookim.accountbookformoms.domain.budget.dao.BudgetRepository;
import com.chaewookim.accountbookformoms.domain.budget.dto.request.BudgetRequest;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.BudgetCopyResponse;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.BudgetResponse;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.BudgetSummaryResponse;
import com.chaewookim.accountbookformoms.domain.budget.entity.Budget;
import com.chaewookim.accountbookformoms.domain.budget.error.BudgetErrorCode;
import com.chaewookim.accountbookformoms.domain.asset.entity.TransactionCategory;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

        Optional<Budget> deletedBudget = budgetRepository.findByUserIdAndYearMonthAndCategoryIdIncludingDeleted(
                userId, request.yearMonth(), request.categoryId());
        if (deletedBudget.isPresent() && deletedBudget.get().getDeletedAt() != null) {
            Budget budget = deletedBudget.get();
            budget.restore();
            budget.update(request.totalBudget(), request.expectedExpense());
            return budgetRepository.save(budget).getId();
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

            Long categoryId = resolveCategoryId(budget);
            String categoryName = resolveCategoryName(budget);

            BigDecimal totalPlannedBudget = budget.getTotalBudget().add(budget.getExpectedExpense());
            BigDecimal actualExpense = expenseMap.getOrDefault(categoryId, BigDecimal.ZERO);
            BigDecimal remaining = totalPlannedBudget.subtract(actualExpense);
            BigDecimal progress = calculateProgress(totalPlannedBudget, actualExpense);

            return new BudgetResponse(
                    budget.getId(),
                    categoryId,
                    categoryName,
                    budget.isCategoryArchived(),
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

    public BudgetCopyResponse previewCopyFromLatest(Long userId, String targetYearMonth) {

        validateTargetMonthEmpty(userId, targetYearMonth);
        CopyPlan plan = buildCopyPlan(userId, targetYearMonth);

        if (plan.sourceBudgets.isEmpty()) {
            throw new CustomException(BudgetErrorCode.NO_SOURCE_BUDGET);
        }

        return toCopyResponse(plan);
    }

    @Transactional
    public BudgetCopyResponse copyFromLatest(Long userId, String targetYearMonth) {

        validateTargetMonthEmpty(userId, targetYearMonth);
        CopyPlan plan = buildCopyPlan(userId, targetYearMonth);

        if (plan.sourceBudgets.isEmpty()) {
            throw new CustomException(BudgetErrorCode.NO_SOURCE_BUDGET);
        }

        List<BudgetCopyResponse.Item> items = new ArrayList<>();
        int copiedCount = 0;

        for (Budget source : plan.sourceBudgets) {
            Long categoryId = resolveCategoryId(source);
            String categoryName = resolveCategoryName(source);
            String skipReason = resolveSkipReason(plan, categoryId);

            if (skipReason != null) {
                items.add(new BudgetCopyResponse.Item(
                        categoryId,
                        categoryName,
                        source.getTotalBudget(),
                        source.getExpectedExpense(),
                        false,
                        skipReason
                ));
                continue;
            }

            BudgetRequest request = new BudgetRequest(
                    categoryId,
                    targetYearMonth,
                    source.getTotalBudget(),
                    source.getExpectedExpense()
            );
            createBudget(userId, request);
            plan.existingTargetCategoryIds.add(categoryId);
            copiedCount++;

            items.add(new BudgetCopyResponse.Item(
                    categoryId,
                    categoryName,
                    source.getTotalBudget(),
                    source.getExpectedExpense(),
                    true,
                    null
            ));
        }

        if (copiedCount > 0) {
            User user = userRepository.findById(userId).orElseThrow();
            user.updateLastBudgetAlertMonth(null);
            userRepository.save(user);
        }

        return new BudgetCopyResponse(plan.sourceYearMonth, targetYearMonth, items, copiedCount);
    }

    private void validateTargetMonthEmpty(Long userId, String targetYearMonth) {
        if (!budgetRepository.findByUserIdAndYearMonth(userId, targetYearMonth).isEmpty()) {
            throw new CustomException(BudgetErrorCode.TARGET_MONTH_NOT_EMPTY);
        }
    }

    private CopyPlan buildCopyPlan(Long userId, String targetYearMonth) {

        String sourceYearMonth = budgetRepository.findLatestBudgetYearMonthBefore(userId, targetYearMonth)
                .orElse(null);
        List<Budget> sourceBudgets = sourceYearMonth == null
                ? List.of()
                : budgetRepository.findByUserIdAndYearMonth(userId, sourceYearMonth);
        Set<Long> activeExpenseCategoryIds = categoryRepository.findAllByUserOrSystem(userId).stream()
                .filter(category -> category.getType() == TransactionType.EXPENSE)
                .map(TransactionCategory::getId)
                .collect(Collectors.toSet());
        Set<Long> existingTargetCategoryIds = budgetRepository.findByUserIdAndYearMonth(userId, targetYearMonth).stream()
                .map(budget -> budget.getTransactionCategory().getId())
                .collect(Collectors.toCollection(HashSet::new));

        return new CopyPlan(sourceYearMonth, targetYearMonth, sourceBudgets, activeExpenseCategoryIds, existingTargetCategoryIds);
    }

    private BudgetCopyResponse toCopyResponse(CopyPlan plan) {

        List<BudgetCopyResponse.Item> items = new ArrayList<>();
        int copyCount = 0;

        for (Budget source : plan.sourceBudgets) {
            Long categoryId = resolveCategoryId(source);
            String categoryName = resolveCategoryName(source);
            String skipReason = resolveSkipReason(plan, categoryId);
            boolean selected = skipReason == null;

            if (selected) {
                copyCount++;
            }

            items.add(new BudgetCopyResponse.Item(
                    categoryId,
                    categoryName,
                    source.getTotalBudget(),
                    source.getExpectedExpense(),
                    selected,
                    skipReason
            ));
        }

        return new BudgetCopyResponse(plan.sourceYearMonth, plan.targetYearMonth, items, copyCount);
    }

    private String resolveSkipReason(CopyPlan plan, Long categoryId) {
        if (!plan.activeExpenseCategoryIds.contains(categoryId)) {
            return "DELETED_CATEGORY";
        }
        if (plan.existingTargetCategoryIds.contains(categoryId)) {
            return "ALREADY_EXISTS";
        }
        return null;
    }

    private record CopyPlan(
            String sourceYearMonth,
            String targetYearMonth,
            List<Budget> sourceBudgets,
            Set<Long> activeExpenseCategoryIds,
            Set<Long> existingTargetCategoryIds
    ) {
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

    private Long resolveCategoryId(Budget budget) {
        if (budget.getTransactionCategory() != null) {
            return budget.getTransactionCategory().getId();
        }
        return budget.getSnapshotCategoryId();
    }

    private String resolveCategoryName(Budget budget) {
        if (budget.getTransactionCategory() != null) {
            return budget.getTransactionCategory().getName();
        }
        if (budget.getSnapshotCategoryName() != null) {
            return budget.getSnapshotCategoryName();
        }
        return "삭제된 카테고리";
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
