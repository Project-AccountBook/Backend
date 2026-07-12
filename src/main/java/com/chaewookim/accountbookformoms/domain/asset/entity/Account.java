package com.chaewookim.accountbookformoms.domain.asset.entity;

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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
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

    @Column
    private BigDecimal goalAmount;

    @Column
    private LocalDate goalDate;

    @Column(nullable = false)
    private boolean goalAchievedNotified;

    @Builder
    public Account(User user, String accountName, BigDecimal initialBalance, AccountRole role) {
        this.user = user;
        this.accountName = accountName;
        this.initialBalance = initialBalance;
        this.currentBalance = initialBalance;
        this.role = role != null ? role : AccountRole.CHECKING;
    }

    public void updateAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void updateInitialBalance(BigDecimal newInitialBalance) {
        BigDecimal delta = newInitialBalance.subtract(this.initialBalance);
        this.initialBalance = newInitialBalance;
        this.currentBalance = this.currentBalance.add(delta);
    }

    public void resetBalance(BigDecimal initialBalance) {
        this.initialBalance = initialBalance;
        this.currentBalance = initialBalance;
    }

    public void changeBalance(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0 && this.currentBalance.add(amount).compareTo(BigDecimal.ZERO) < 0) {
            throw new CustomException(AssetErrorCode.INSUFFICIENT_BALANCE);
        }
        this.currentBalance = this.currentBalance.add(amount);
    }

    public void updateRole(AccountRole role) {
        this.role = role != null ? role : AccountRole.CHECKING;
    }

    public void updateGoal(BigDecimal goalAmount, LocalDate goalDate) {
        if (goalAmount != null && goalAmount.compareTo(BigDecimal.ZERO) <= 0) {
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

    public void markGoalAchievedNotified() {
        this.goalAchievedNotified = true;
    }

    public void resetGoalAchievedNotified() {
        this.goalAchievedNotified = false;
    }
}
