package com.chaewookim.accountbookformoms.domain.budget.entity;

import com.chaewookim.accountbookformoms.domain.asset.entity.TransactionCategory;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@SQLDelete(sql = "UPDATE budget SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Table(name = "budget", uniqueConstraints = {
        @UniqueConstraint(name = "uk_budget_user_month_category",
                columnNames = {"user_id", "`year_month`", "category_id"})
})
public class Budget extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private TransactionCategory transactionCategory;

    @Column(nullable = false, name = "`year_month`")
    private String yearMonth;

    @Column(nullable = false)
    private BigDecimal totalBudget;

    @Column(nullable = false)
    private BigDecimal expectedExpense;

    @Builder
    public Budget(User user, TransactionCategory transactionCategory, String yearMonth, BigDecimal totalBudget, BigDecimal expectedExpense) {
        this.user = user;
        this.transactionCategory = transactionCategory;
        this.yearMonth = yearMonth;
        this.totalBudget = totalBudget;
        this.expectedExpense = expectedExpense;
    }
}
