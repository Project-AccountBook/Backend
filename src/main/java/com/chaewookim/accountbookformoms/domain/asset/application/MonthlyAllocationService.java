package com.chaewookim.accountbookformoms.domain.asset.application;

import com.chaewookim.accountbookformoms.domain.asset.dao.AccountRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionCategoryRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.dto.response.AllocationBucketResponse;
import com.chaewookim.accountbookformoms.domain.asset.dto.response.GoalProgressResponse;
import com.chaewookim.accountbookformoms.domain.asset.dto.response.MonthlyAllocationResponse;
import com.chaewookim.accountbookformoms.domain.asset.dto.response.MonthlyAllocationSummaryResponse;
import com.chaewookim.accountbookformoms.domain.asset.entity.Account;
import com.chaewookim.accountbookformoms.domain.asset.entity.Transaction;
import com.chaewookim.accountbookformoms.domain.asset.entity.TransactionCategory;
import com.chaewookim.accountbookformoms.domain.asset.enums.AccountRole;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
import com.chaewookim.accountbookformoms.domain.portfolio.application.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonthlyAllocationService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionCategoryRepository categoryRepository;
    private final PortfolioService portfolioService;

    public MonthlyAllocationSummaryResponse getMonthlyAllocationSummary(Long userId, String yearMonth) {

        BigDecimal totalIncome = portfolioService.getMyPortfolio(userId, yearMonth).totalIncome();
        MonthlyAllocationResponse allocation = computeMonthlyAllocation(userId, yearMonth, totalIncome);

        return MonthlyAllocationSummaryResponse.from(yearMonth, totalIncome, allocation);
    }

    public MonthlyAllocationResponse computeMonthlyAllocation(Long userId, String yearMonth, BigDecimal totalIncome) {

        YearMonth month = YearMonth.parse(yearMonth);
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        List<Account> accounts = accountRepository.findByUserId(userId);
        Map<Long, AccountRole> roleMap = new HashMap<>();
        for (Account account : accounts) {
            roleMap.put(account.getId(), account.getRole());
        }

        Map<Long, CategoryAllocationFlags> categoryFlags = buildCategoryFlags(userId);

        BigDecimal savingsInflow = BigDecimal.ZERO;
        BigDecimal savingsOutflow = BigDecimal.ZERO;
        BigDecimal investmentInflow = BigDecimal.ZERO;
        BigDecimal investmentOutflow = BigDecimal.ZERO;

        List<Transaction> transfers = transactionRepository.findByUserIdAndDateBetween(userId, startDate, endDate)
                .stream()
                .filter(tx -> tx.getType() == TransactionType.TRANSFER)
                .toList();

        for (Transaction tx : transfers) {
            BigDecimal amount = tx.getAmount().abs();
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Long sourceAccountId = resolveAccountId(tx.getAccount(), tx.getSnapshotAccountId());
            if (sourceAccountId == null) {
                continue;
            }

            AccountRole sourceRole = roleMap.getOrDefault(sourceAccountId, AccountRole.CHECKING);

            Long targetAccountId = resolveAccountId(tx.getTargetAccount(), tx.getSnapshotTargetAccountId());
            AccountRole targetRole = targetAccountId != null
                    ? roleMap.getOrDefault(targetAccountId, AccountRole.CHECKING)
                    : AccountRole.CHECKING;

            CategoryAllocationFlags flags = new CategoryAllocationFlags(false, false);
            if (tx.getTransactionCategory() != null) {
                flags = categoryFlags.getOrDefault(
                        tx.getTransactionCategory().getId(),
                        flags
                );
            }

            if (targetRole == AccountRole.SAVINGS) {
                savingsInflow = savingsInflow.add(amount);
            } else if (targetRole == AccountRole.INVESTMENT) {
                investmentInflow = investmentInflow.add(amount);
            } else if (flags.includeInSavingsRate()) {
                savingsInflow = savingsInflow.add(amount);
            } else if (flags.includeInInvestmentRate()) {
                investmentInflow = investmentInflow.add(amount);
            }

            if (sourceRole == AccountRole.SAVINGS) {
                savingsOutflow = savingsOutflow.add(amount);
            }
            if (sourceRole == AccountRole.INVESTMENT) {
                investmentOutflow = investmentOutflow.add(amount);
            }
        }

        return new MonthlyAllocationResponse(
                buildBucket(savingsInflow, savingsOutflow, totalIncome),
                buildBucket(investmentInflow, investmentOutflow, totalIncome)
        );
    }

    public List<GoalProgressResponse> buildGoalProgress(Long userId) {
        return accountRepository.findByUserId(userId).stream()
                .filter(account -> account.getGoalAmount() != null
                        && account.getGoalAmount().compareTo(BigDecimal.ZERO) > 0)
                .map(this::toGoalProgress)
                .sorted(Comparator.comparing(GoalProgressResponse::progressPercent).reversed())
                .toList();
    }

    public BigDecimal computeTotalAsset(Long userId) {
        return accountRepository.findByUserId(userId).stream()
                .map(Account::getCurrentBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private GoalProgressResponse toGoalProgress(Account account) {

        BigDecimal goalAmount = account.getGoalAmount();
        BigDecimal balance = account.getCurrentBalance() != null ? account.getCurrentBalance() : BigDecimal.ZERO;
        Integer progressPercent = calculateProgressPercent(balance, goalAmount);
        LocalDate goalDate = account.getGoalDate();

        return new GoalProgressResponse(
                account.getId(),
                account.getAccountName(),
                account.getRole(),
                balance,
                goalAmount,
                progressPercent,
                goalDate,
                calculateDDay(goalDate)
        );
    }

    private Integer calculateProgressPercent(BigDecimal currentBalance, BigDecimal goalAmount) {

        if (goalAmount == null || goalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        int percent = currentBalance
                .multiply(BigDecimal.valueOf(100))
                .divide(goalAmount, 0, RoundingMode.HALF_UP)
                .intValue();

        return Math.min(100, Math.max(0, percent));
    }

    private Integer calculateDDay(LocalDate goalDate) {

        if (goalDate == null) {
            return null;
        }

        return (int) ChronoUnit.DAYS.between(LocalDate.now(), goalDate);
    }

    private Map<Long, CategoryAllocationFlags> buildCategoryFlags(Long userId) {

        Map<Long, CategoryAllocationFlags> flags = new HashMap<>();

        for (TransactionCategory category : categoryRepository.findAllByUserOrSystem(userId)) {
            flags.put(
                    category.getId(),
                    new CategoryAllocationFlags(
                            category.isIncludeInSavingsRate(),
                            category.isIncludeInInvestmentRate()
                    )
            );
        }

        return flags;
    }

    private AllocationBucketResponse buildBucket(BigDecimal inflow, BigDecimal outflow, BigDecimal totalIncome) {

        BigDecimal net = inflow.subtract(outflow);
        BigDecimal rate = BigDecimal.ZERO;

        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            rate = net.multiply(BigDecimal.valueOf(100))
                    .divide(totalIncome, 1, RoundingMode.HALF_UP);
        }

        return new AllocationBucketResponse(net, rate, inflow, outflow);
    }

    private Long resolveAccountId(Account account, Long snapshotAccountId) {
        if (account != null) {
            return account.getId();
        }
        return snapshotAccountId;
    }

    private record CategoryAllocationFlags(boolean includeInSavingsRate, boolean includeInInvestmentRate) {
    }
}
