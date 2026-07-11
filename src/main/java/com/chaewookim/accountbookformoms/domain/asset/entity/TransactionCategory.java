package com.chaewookim.accountbookformoms.domain.asset.entity;

import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.entity.BaseEntity;
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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE transaction_category SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class TransactionCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(nullable = false)
    private boolean includeInSavingsRate = false;

    @Column(nullable = false)
    private boolean includeInInvestmentRate = false;

    @Builder
    public TransactionCategory(
            User user,
            String name,
            TransactionType type,
            Boolean includeInSavingsRate,
            Boolean includeInInvestmentRate
    ) {
        this.user = user;
        this.name = name;
        this.type = type;
        applyAllocationFlags(type, includeInSavingsRate, includeInInvestmentRate);
    }

    public void update(String name, TransactionType transactionType, Boolean includeInSavingsRate, Boolean includeInInvestmentRate) {
        TransactionType previousType = this.type;
        this.name = name;
        this.type = transactionType;

        if (transactionType != TransactionType.TRANSFER) {
            this.includeInSavingsRate = false;
            this.includeInInvestmentRate = false;
            return;
        }

        if (includeInSavingsRate != null) {
            this.includeInSavingsRate = includeInSavingsRate;
        } else if (previousType != TransactionType.TRANSFER) {
            this.includeInSavingsRate = true;
        }

        if (includeInInvestmentRate != null) {
            this.includeInInvestmentRate = includeInInvestmentRate;
        } else if (previousType != TransactionType.TRANSFER) {
            this.includeInInvestmentRate = false;
        }
    }

    public void updateAllocationFlags(boolean includeInSavingsRate, boolean includeInInvestmentRate) {
        if (this.type != TransactionType.TRANSFER) {
            this.includeInSavingsRate = false;
            this.includeInInvestmentRate = false;
            return;
        }
        this.includeInSavingsRate = includeInSavingsRate;
        this.includeInInvestmentRate = includeInInvestmentRate;
    }

    private void applyAllocationFlags(TransactionType type, Boolean includeInSavingsRate, Boolean includeInInvestmentRate) {
        if (type != TransactionType.TRANSFER) {
            this.includeInSavingsRate = false;
            this.includeInInvestmentRate = false;
            return;
        }
        this.includeInSavingsRate = includeInSavingsRate != null ? includeInSavingsRate : true;
        this.includeInInvestmentRate = includeInInvestmentRate != null ? includeInInvestmentRate : false;
    }
}
