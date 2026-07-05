package com.chaewookim.accountbookformoms.domain.asset.entity;

import com.chaewookim.accountbookformoms.domain.asset.error.AssetErrorCode;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.entity.BaseEntity;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Builder
    public Account(User user, String accountName, BigDecimal initialBalance) {
        this.user = user;
        this.accountName = accountName;
        this.initialBalance = initialBalance;
        this.currentBalance = initialBalance;
    }

    public void updateAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void updateInitialBalance(BigDecimal newInitialBalance) {
        BigDecimal delta = newInitialBalance.subtract(this.initialBalance);
        this.initialBalance = newInitialBalance;
        this.currentBalance = this.currentBalance.add(delta);
    }

    public void changeBalance(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0 && this.currentBalance.add(amount).compareTo(BigDecimal.ZERO) < 0) {
            throw new CustomException(AssetErrorCode.INSUFFICIENT_BALANCE);
        }
        this.currentBalance = this.currentBalance.add(amount);
    }
}
