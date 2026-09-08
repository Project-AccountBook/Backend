package com.chaewookim.accountbookformoms.domain.asset.application;

import com.chaewookim.accountbookformoms.domain.asset.dao.AccountRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.dto.response.AllocationBucketResponse;
import com.chaewookim.accountbookformoms.domain.asset.dto.response.GoalProgressResponse;
import com.chaewookim.accountbookformoms.domain.asset.dto.response.MonthlyAllocationResponse;
import com.chaewookim.accountbookformoms.domain.asset.dto.response.MonthlyAllocationSummaryResponse;
import com.chaewookim.accountbookformoms.domain.asset.entity.Account;
import com.chaewookim.accountbookformoms.domain.asset.entity.Transaction;
import com.chaewookim.accountbookformoms.domain.asset.enums.AccountKind;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonthlyAllocationService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
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

            AccountRole sourceRole = resolveRole(tx.getSnapshotAccountRole(), tx.getAccount());
            AccountRole targetRole = resolveRole(tx.getSnapshotTargetAccountRole(), tx.getTargetAccount());

            if (targetRole == AccountRole.SAVINGS) {
                savingsInflow = savingsInflow.add(amount);
            } else if (targetRole == AccountRole.INVESTMENT) {
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
                .filter(account -> account.getKind() == AccountKind.ASSET
                        || account.getKind() == AccountKind.LOAN)
                .filter(account -> account.getGoalAmount() != null)
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
        Integer progressPercent = account.calculateGoalProgressPercent();
        LocalDate goalDate = account.getGoalDate();

        return new GoalProgressResponse(
                account.getId(),
                account.getAccountName(),
                account.getKind(),
                account.getRole(),
                balance,
                goalAmount,
                progressPercent,
                goalDate,
                calculateDDay(goalDate)
        );
    }

    private Integer calculateDDay(LocalDate goalDate) {

        if (goalDate == null) {
            return null;
        }

        return (int) ChronoUnit.DAYS.between(LocalDate.now(), goalDate);
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

    private AccountRole resolveRole(AccountRole snapshotRole, Account account) {
        if (snapshotRole != null) {
            return snapshotRole;
        }
        if (account != null && account.getRole() != null) {
            return account.getRole();
        }
        return AccountRole.CHECKING;
    }

}
