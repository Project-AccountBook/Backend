package com.chaewookim.accountbookformoms.domain.budget.application;

import com.chaewookim.accountbookformoms.domain.asset.dao.FixedTransactionRepository;
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
import java.util.HashMap;
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
    private final FixedTransactionRepository fixedTransactionRepository;
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
        return buildBudgetViewContexts(userId, yearMonth).stream()
                .map(this::toBudgetResponse)
                .toList();
    }

    public BudgetSummaryResponse getMonthlyBudgetSummary(Long userId, String yearMonth) {

        List<BudgetViewContext> contexts = buildBudgetViewContexts(userId, yearMonth);

        BigDecimal totalPlannedBudgetSum = contexts.stream()
                .map(BudgetViewContext::totalPlannedBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate[] monthRange = monthDateRange(yearMonth);
        BigDecimal totalActualExpenseSum = transactionRepository.sumByUserAndType(
                userId, TransactionType.EXPENSE, monthRange[0], monthRange[1]);
        if (totalActualExpenseSum == null) {
            totalActualExpenseSum = BigDecimal.ZERO;
        }

        BigDecimal totalRemainingBudget = totalPlannedBudgetSum.subtract(totalActualExpenseSum);

        return new BudgetSummaryResponse(yearMonth, totalPlannedBudgetSum, totalActualExpenseSum, totalRemainingBudget);
    }

    private boolean isUserConfiguredBudget(Budget budget) {
        return budget.getTotalBudget().compareTo(BigDecimal.ZERO) > 0
                || budget.getExpectedExpense().compareTo(BigDecimal.ZERO) > 0;
    }

    private void removePlaceholderBudgets(Long userId, String yearMonth) {
        budgetRepository.findByUserIdAndYearMonth(userId, yearMonth).stream()
                .filter(budget -> !isUserConfiguredBudget(budget))
                .forEach(budgetRepository::delete);
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

        validateTargetMonthCanCopyFromLatest(userId, targetYearMonth);
        CopyPlan plan = buildCopyPlan(userId, targetYearMonth);

        if (plan.sourceBudgets.isEmpty()) {
            throw new CustomException(BudgetErrorCode.NO_SOURCE_BUDGET);
        }

        return toCopyResponse(plan);
    }

    @Transactional
    public BudgetCopyResponse copyFromLatest(Long userId, String targetYearMonth) {

        validateTargetMonthCanCopyFromLatest(userId, targetYearMonth);
        removePlaceholderBudgets(userId, targetYearMonth);
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

    private void validateTargetMonthCanCopyFromLatest(Long userId, String targetYearMonth) {
        boolean hasUserConfiguredBudget = budgetRepository.findByUserIdAndYearMonth(userId, targetYearMonth).stream()
                .anyMatch(this::isUserConfiguredBudget);
        if (hasUserConfiguredBudget) {
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
                .filter(this::isUserConfiguredBudget)
                .map(this::resolveCategoryId)
                .filter(java.util.Objects::nonNull)
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

        LocalDate[] monthRange = monthDateRange(yearMonth);
        BigDecimal actualExpense = transactionRepository.sumAmountByUserIdAndCategoryId(
                userId, categoryId, monthRange[0], monthRange[1]);
        if (actualExpense == null) {
            actualExpense = BigDecimal.ZERO;
        }

        Optional<Budget> budget = budgetRepository.findByUserIdAndYearMonthAndTransactionCategoryId(
                userId, yearMonth, categoryId);
        BigDecimal fixedExpenseAmount = calculateFixedExpenseForCategory(userId, categoryId, yearMonth);
        BigDecimal totalBudget = budget.map(Budget::getTotalBudget).orElse(BigDecimal.ZERO);
        BigDecimal expectedExpense = budget.map(Budget::getExpectedExpense).orElse(BigDecimal.ZERO);
        BigDecimal totalPlanned = totalBudget.add(fixedExpenseAmount).add(expectedExpense);

        if (totalPlanned.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return calculateProgress(totalPlanned, actualExpense);
    }

    private List<BudgetViewContext> buildBudgetViewContexts(Long userId, String yearMonth) {

        List<Budget> budgets = budgetRepository.findByUserIdAndYearMonth(userId, yearMonth);
        LocalDate[] monthRange = monthDateRange(yearMonth);
        FixedExpenseSnapshot fixedSnapshot = buildFixedExpenseSnapshot(userId, monthRange[0], monthRange[1]);

        Map<Long, BigDecimal> expenseMap = transactionRepository.sumAmountByUserIdGroupByCategoryId(userId, yearMonth)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (BigDecimal) row[1]
                ));

        List<BudgetViewContext> contexts = budgets.stream()
                .map(budget -> {
                    Long categoryId = resolveCategoryId(budget);
                    BigDecimal fixedExpenseAmount = fixedSnapshot.amountByCategory()
                            .getOrDefault(categoryId, BigDecimal.ZERO);
                    return new BudgetViewContext(
                            budget.getId(),
                            categoryId,
                            resolveCategoryName(budget),
                            budget.isCategoryArchived(),
                            budget.getTotalBudget(),
                            fixedExpenseAmount,
                            budget.getExpectedExpense(),
                            expenseMap.getOrDefault(categoryId, BigDecimal.ZERO)
                    );
                })
                .collect(Collectors.toCollection(ArrayList::new));

        Set<Long> coveredCategoryIds = budgets.stream()
                .map(this::resolveCategoryId)
                .collect(Collectors.toSet());

        fixedSnapshot.amountByCategory().forEach((categoryId, fixedAmount) -> {
            if (coveredCategoryIds.contains(categoryId) || fixedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return;
            }
            contexts.add(new BudgetViewContext(
                    null,
                    categoryId,
                    fixedSnapshot.nameByCategory().getOrDefault(categoryId, "카테고리"),
                    false,
                    BigDecimal.ZERO,
                    fixedAmount,
                    BigDecimal.ZERO,
                    expenseMap.getOrDefault(categoryId, BigDecimal.ZERO)
            ));
        });

        return contexts;
    }

    private BudgetResponse toBudgetResponse(BudgetViewContext context) {
        BigDecimal totalPlannedBudget = context.totalPlannedBudget();
        BigDecimal remaining = totalPlannedBudget.subtract(context.actualExpense());
        BigDecimal progress = calculateProgress(totalPlannedBudget, context.actualExpense());

        return new BudgetResponse(
                context.id(),
                context.categoryId(),
                context.categoryName(),
                context.categoryArchived(),
                context.totalBudget(),
                context.fixedExpenseAmount(),
                context.expectedExpense(),
                totalPlannedBudget,
                context.actualExpense(),
                remaining,
                progress
        );
    }

    private FixedExpenseSnapshot buildFixedExpenseSnapshot(Long userId, LocalDate startDate, LocalDate endDate) {

        Map<Long, BigDecimal> amountByCategory = new HashMap<>();
        Map<Long, String> nameByCategory = new HashMap<>();

        for (Object[] row : fixedTransactionRepository.sumByUserCategory(
                userId, TransactionType.EXPENSE, startDate, endDate)) {
            Long categoryId = (Long) row[0];
            String categoryName = (String) row[1];
            BigDecimal amount = (BigDecimal) row[2];
            amountByCategory.put(categoryId, amount);
            nameByCategory.put(categoryId, categoryName);
        }

        return new FixedExpenseSnapshot(amountByCategory, nameByCategory);
    }

    private BigDecimal calculateFixedExpenseForCategory(Long userId, Long categoryId, String yearMonth) {

        LocalDate[] monthRange = monthDateRange(yearMonth);
        BigDecimal fixedAmount = fixedTransactionRepository.sumByUserAndTypeAndCategory(
                userId,
                TransactionType.EXPENSE,
                categoryId,
                monthRange[0],
                monthRange[1]
        );
        return fixedAmount != null ? fixedAmount : BigDecimal.ZERO;
    }

    private LocalDate[] monthDateRange(String yearMonth) {
        LocalDate startDate = LocalDate.parse(yearMonth + "-01");
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        return new LocalDate[]{startDate, endDate};
    }

    private record FixedExpenseSnapshot(
            Map<Long, BigDecimal> amountByCategory,
            Map<Long, String> nameByCategory
    ) {
    }

    private record BudgetViewContext(
            Long id,
            Long categoryId,
            String categoryName,
            boolean categoryArchived,
            BigDecimal totalBudget,
            BigDecimal fixedExpenseAmount,
            BigDecimal expectedExpense,
            BigDecimal actualExpense
    ) {
        BigDecimal totalPlannedBudget() {
            return totalBudget.add(fixedExpenseAmount).add(expectedExpense);
        }
    }
}
