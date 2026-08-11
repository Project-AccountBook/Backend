package com.chaewookim.accountbookformoms.domain.asset.entity;

import com.chaewookim.accountbookformoms.domain.asset.enums.AccountKind;
import com.chaewookim.accountbookformoms.domain.asset.enums.AccountRole;
import com.chaewookim.accountbookformoms.domain.asset.error.AssetErrorCode;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.entity.BaseEntity;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Getter
@Table(name = "account",
        indexes = {
                @Index(name = "idx_account_user", columnList = "user_id"),
                @Index(name = "idx_account_user_name", columnList = "user_id, account_name")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE account SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Account extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String accountName;

    @Column(nullable = false)
    private BigDecimal initialBalance;

    @Column(nullable = false)
    private BigDecimal currentBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountRole role = AccountRole.CHECKING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountKind kind = AccountKind.ASSET;

    @Column
    private BigDecimal creditLimit;

    @Column
    private BigDecimal loanLimit;

    @Column
    private BigDecimal disbursedAmount;

    @Column
    private BigDecimal goalAmount;

    @Column
    private LocalDate goalDate;

    @Column(nullable = false)
    private boolean goalAchievedNotified;

    @Builder
    public Account(User user, String accountName, BigDecimal initialBalance, AccountRole role,
                   AccountKind kind, BigDecimal creditLimit, BigDecimal loanLimit,
                   BigDecimal disbursedAmount) {
        this.user = user;
        this.accountName = accountName;
        this.initialBalance = initialBalance;
        this.currentBalance = initialBalance;
        this.kind = kind != null ? kind : AccountKind.ASSET;
        this.role = this.kind == AccountKind.ASSET && role != null ? role : AccountRole.CHECKING;
        this.creditLimit = creditLimit;
        this.loanLimit = this.kind == AccountKind.LOAN
                ? (loanLimit != null ? loanLimit : absoluteOrZero(initialBalance))
                : loanLimit;
        this.disbursedAmount = this.kind == AccountKind.LOAN
                ? (disbursedAmount != null ? disbursedAmount : this.loanLimit)
                : disbursedAmount;
        validateAccountPolicy(initialBalance, initialBalance, this.kind, creditLimit,
                this.loanLimit, this.disbursedAmount);
    }

    public void updateAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void updateInitialBalance(BigDecimal newInitialBalance) {
        BigDecimal delta = newInitialBalance.subtract(this.initialBalance);
        this.initialBalance = newInitialBalance;
        this.currentBalance = this.currentBalance.add(delta);
    }

    public void updateInitialBalanceOnly(BigDecimal newInitialBalance) {
        this.initialBalance = newInitialBalance;
    }

    public void resetBalance(BigDecimal initialBalance) {
        validateAccountPolicy(initialBalance, initialBalance, this.kind, this.creditLimit,
                this.loanLimit, this.disbursedAmount);
        this.initialBalance = initialBalance;
        this.currentBalance = initialBalance;
    }

    public void changeBalance(BigDecimal amount) {
        BigDecimal nextBalance = this.currentBalance.add(amount);
        if (this.kind == AccountKind.ASSET && nextBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new CustomException(AssetErrorCode.INSUFFICIENT_BALANCE);
        }
        if (this.kind == AccountKind.CREDIT_CARD
                && this.creditLimit != null
                && nextBalance.compareTo(this.creditLimit.negate()) < 0) {
            throw new CustomException(AssetErrorCode.CREDIT_LIMIT_EXCEEDED);
        }
        if (this.kind != AccountKind.ASSET && nextBalance.compareTo(BigDecimal.ZERO) > 0) {
            throw new CustomException(AssetErrorCode.INVALID_LIABILITY_BALANCE);
        }
        if (this.kind == AccountKind.LOAN
                && nextBalance.abs().compareTo(this.disbursedAmount) > 0) {
            throw new CustomException(AssetErrorCode.LOAN_DEBT_EXCEEDS_DISBURSED_AMOUNT);
        }
        this.currentBalance = nextBalance;
    }

    public void disburse(BigDecimal amount) {
        BigDecimal nextDisbursedAmount = this.disbursedAmount.add(amount);
        if (nextDisbursedAmount.compareTo(this.loanLimit) > 0) {
            throw new CustomException(AssetErrorCode.LOAN_LIMIT_EXCEEDED);
        }
        this.currentBalance = this.currentBalance.subtract(amount);
        this.disbursedAmount = nextDisbursedAmount;
    }

    public void reverseDisbursement(BigDecimal amount) {
        BigDecimal nextBalance = this.currentBalance.add(amount);
        BigDecimal nextDisbursedAmount = this.disbursedAmount.subtract(amount);
        if (nextDisbursedAmount.compareTo(BigDecimal.ZERO) < 0
                || nextBalance.compareTo(BigDecimal.ZERO) > 0
                || nextBalance.abs().compareTo(nextDisbursedAmount) > 0) {
            throw new CustomException(AssetErrorCode.INVALID_LOAN_REVERSAL);
        }
        this.currentBalance = nextBalance;
        this.disbursedAmount = nextDisbursedAmount;
    }

    public void updateRole(AccountRole role) {
        this.role = this.kind == AccountKind.ASSET && role != null ? role : AccountRole.CHECKING;
    }

    public void reinitialize(AccountKind kind, BigDecimal creditLimit, BigDecimal loanLimit,
                             BigDecimal disbursedAmount, BigDecimal initialBalance, AccountRole role) {
        AccountKind resolvedKind = kind != null ? kind : AccountKind.ASSET;
        validateAccountPolicy(initialBalance, initialBalance, resolvedKind, creditLimit,
                loanLimit, disbursedAmount);
        if (this.kind != resolvedKind) {
            clearGoal();
        }
        this.kind = resolvedKind;
        this.creditLimit = creditLimit;
        this.loanLimit = loanLimit;
        this.disbursedAmount = disbursedAmount;
        this.initialBalance = initialBalance;
        this.currentBalance = initialBalance;
        this.role = resolvedKind == AccountKind.ASSET && role != null ? role : AccountRole.CHECKING;
    }

    public void updateKindAndLimits(AccountKind kind, BigDecimal creditLimit, BigDecimal loanLimit,
                                    BigDecimal initialBalance, BigDecimal currentBalance) {
        AccountKind resolvedKind = kind != null ? kind : AccountKind.ASSET;
        AccountKind previousKind = this.kind;
        BigDecimal resolvedLoanLimit = null;
        BigDecimal resolvedDisbursedAmount = null;
        if (resolvedKind == AccountKind.LOAN) {
            resolvedLoanLimit = loanLimit != null
                    ? loanLimit
                    : (previousKind == AccountKind.LOAN ? this.loanLimit : absoluteOrZero(initialBalance));
            resolvedDisbursedAmount = previousKind == AccountKind.LOAN
                    ? this.disbursedAmount
                    : absoluteOrZero(currentBalance);
        }
        validateAccountPolicy(initialBalance, currentBalance, resolvedKind, creditLimit,
                resolvedLoanLimit, resolvedDisbursedAmount);
        this.kind = resolvedKind;
        this.creditLimit = creditLimit;
        this.loanLimit = resolvedLoanLimit;
        this.disbursedAmount = resolvedDisbursedAmount;
        if (previousKind != null && previousKind != resolvedKind) {
            clearGoal();
        }
        if (resolvedKind != AccountKind.ASSET) {
            this.role = AccountRole.CHECKING;
        }
    }

    private void validateAccountPolicy(BigDecimal initialBalance, BigDecimal currentBalance,
                                       AccountKind kind, BigDecimal creditLimit,
                                       BigDecimal loanLimit, BigDecimal disbursedAmount) {
        if (creditLimit != null && creditLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new CustomException(AssetErrorCode.INVALID_CREDIT_LIMIT);
        }
        if (kind != AccountKind.CREDIT_CARD && creditLimit != null) {
            throw new CustomException(AssetErrorCode.CREDIT_LIMIT_NOT_ALLOWED);
        }
        if (kind == AccountKind.ASSET
                && ((initialBalance != null && initialBalance.compareTo(BigDecimal.ZERO) < 0)
                || (currentBalance != null && currentBalance.compareTo(BigDecimal.ZERO) < 0))) {
            throw new CustomException(AssetErrorCode.INVALID_ACCOUNT_BALANCE);
        }
        if (kind != AccountKind.ASSET
                && ((initialBalance != null && initialBalance.compareTo(BigDecimal.ZERO) > 0)
                || (currentBalance != null && currentBalance.compareTo(BigDecimal.ZERO) > 0))) {
            throw new CustomException(AssetErrorCode.INVALID_LIABILITY_BALANCE);
        }
        if (kind == AccountKind.CREDIT_CARD && creditLimit != null
                && ((initialBalance != null && initialBalance.compareTo(creditLimit.negate()) < 0)
                || (currentBalance != null && currentBalance.compareTo(creditLimit.negate()) < 0))) {
            throw new CustomException(AssetErrorCode.CREDIT_LIMIT_EXCEEDED);
        }
        if (kind != AccountKind.LOAN && (loanLimit != null || disbursedAmount != null)) {
            throw new CustomException(AssetErrorCode.LOAN_FIELDS_NOT_ALLOWED);
        }
        if (kind == AccountKind.LOAN) {
            if (loanLimit == null || loanLimit.compareTo(BigDecimal.ZERO) < 0) {
                throw new CustomException(AssetErrorCode.INVALID_LOAN_LIMIT);
            }
            if (disbursedAmount == null || disbursedAmount.compareTo(BigDecimal.ZERO) < 0
                    || disbursedAmount.compareTo(loanLimit) > 0) {
                throw new CustomException(AssetErrorCode.INVALID_DISBURSED_AMOUNT);
            }
            if (currentBalance != null && currentBalance.abs().compareTo(disbursedAmount) > 0) {
                throw new CustomException(AssetErrorCode.LOAN_DEBT_EXCEEDS_DISBURSED_AMOUNT);
            }
        }
    }

    private static BigDecimal absoluteOrZero(BigDecimal amount) {
        return amount != null ? amount.abs() : BigDecimal.ZERO;
    }

    public void updateGoal(BigDecimal goalAmount, LocalDate goalDate) {
        if (this.kind == AccountKind.CREDIT_CARD) {
            throw new CustomException(AssetErrorCode.GOAL_NOT_SUPPORTED);
        }
        if (this.kind == AccountKind.LOAN
                && (goalAmount == null || goalAmount.compareTo(BigDecimal.ZERO) < 0)) {
            throw new CustomException(AssetErrorCode.INVALID_DEBT_GOAL_AMOUNT);
        }
        if (this.kind == AccountKind.ASSET
                && (goalAmount == null || goalAmount.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new CustomException(AssetErrorCode.INVALID_GOAL_AMOUNT);
        }
        this.goalAmount = goalAmount;
        this.goalDate = goalDate;
        this.goalAchievedNotified = false;
    }

    public void clearGoal() {
        this.goalAmount = null;
        this.goalDate = null;
        this.goalAchievedNotified = false;
    }

    public void resetGoalAchievedNotified() {
        this.goalAchievedNotified = false;
    }

    public boolean isReadyForNotification() {
        return goalAmount != null && isGoalAchieved() && !isGoalAchievedNotified();
    }

    public boolean isGoalAchieved() {
        if (goalAmount == null || this.kind == AccountKind.CREDIT_CARD) {
            return false;
        }
        if (this.kind == AccountKind.LOAN) {
            BigDecimal remainingDebt = currentBalance != null ? currentBalance.abs() : BigDecimal.ZERO;
            return remainingDebt.compareTo(goalAmount) <= 0;
        }
        if (goalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        BigDecimal balance = currentBalance != null ? currentBalance : BigDecimal.ZERO;
        if (balance.compareTo(goalAmount) >= 0) {
            return true;
        }
        int percent = balance
                .multiply(BigDecimal.valueOf(100))
                .divide(goalAmount, 0, RoundingMode.HALF_UP)
                .intValue();
        return percent >= 100;
    }

    public Integer calculateGoalProgressPercent() {
        if (goalAmount == null || this.kind == AccountKind.CREDIT_CARD) {
            return null;
        }
        if (this.kind == AccountKind.LOAN) {
            BigDecimal initialDebt = disbursedAmount != null ? disbursedAmount : BigDecimal.ZERO;
            BigDecimal currentDebt = currentBalance != null ? currentBalance.abs() : BigDecimal.ZERO;
            BigDecimal amountToRepay = initialDebt.subtract(goalAmount);
            if (amountToRepay.compareTo(BigDecimal.ZERO) <= 0) {
                return currentDebt.compareTo(goalAmount) <= 0 ? 100 : 0;
            }
            int percent = initialDebt.subtract(currentDebt)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(amountToRepay, 0, RoundingMode.HALF_UP)
                    .intValue();
            return Math.min(100, Math.max(0, percent));
        }
        if (goalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal balance = currentBalance != null ? currentBalance : BigDecimal.ZERO;
        int percent = balance
                .multiply(BigDecimal.valueOf(100))
                .divide(goalAmount, 0, RoundingMode.HALF_UP)
                .intValue();
        return Math.min(100, Math.max(0, percent));
    }
}
