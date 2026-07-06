package com.chaewookim.accountbookformoms.domain.asset.entity;

import com.chaewookim.accountbookformoms.domain.asset.dto.request.TransactionRequest;
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
import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE transaction SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Table(name = "transaction", indexes = {
        @Index(name = "idx_transaction_user_account_date",
                columnList = "user_id, account_id, transaction_date")
})
public class Transaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_account_id")
    private Account targetAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private TransactionCategory transactionCategory;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate transactionDate;

    private String description;

    @Builder
    public Transaction(User user, Account account, Account targetAccount, TransactionCategory transactionCategory,
                       TransactionType type, BigDecimal amount, LocalDate transactionDate, String description) {
        this.user = user;
        this.account = account;
        this.targetAccount = targetAccount;
        this.transactionCategory = transactionCategory;
        this.type = type;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.description = description;
    }

    public void update(TransactionRequest request, TransactionCategory category, Account account, Account targetAccount) {
        this.account = account;
        this.transactionCategory = category;
        this.type = request.type();
        this.amount = request.amount();
        this.transactionDate = request.transactionDate();
        this.description = request.description();
        this.targetAccount = request.type() == TransactionType.TRANSFER ? targetAccount : null;
    }

    public BigDecimal getBalanceChangeAmount() {
        return (this.type == TransactionType.EXPENSE || this.type == TransactionType.TRANSFER)
                ? this.amount.negate()
                : this.amount;
    }
}
