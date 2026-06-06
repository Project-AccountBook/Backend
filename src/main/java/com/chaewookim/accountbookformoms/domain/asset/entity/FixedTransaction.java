package com.chaewookim.accountbookformoms.domain.asset.entity;

import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionFrequency;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
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
@SQLDelete(sql = "UPDATE fixed_transaction SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class FixedTransaction extends BaseEntity {

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
    @JoinColumn(name = "category_id")
    private TransactionCategory transactionCategory;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionFrequency frequency;

    @Column(nullable = false)
    private Integer repeatDay;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDate nextExecutionDate;    // 다음 실행 예정일 (배치 최적화용)

    private String description;

    @Column(nullable = false)
    private Boolean isActive;

    @Builder
    public FixedTransaction(User user, Account account, TransactionCategory transactionCategory, TransactionType type,
                            BigDecimal amount, TransactionFrequency frequency, Integer repeatDay,
                            LocalDate startDate, LocalDate endDate, String description) {
        this.user = user;
        this.account = account;
        this.transactionCategory = transactionCategory;
        this.type = type;
        this.amount = amount;
        this.frequency = frequency;
        this.repeatDay = repeatDay;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
        this.nextExecutionDate = calculateInitialNextDate(startDate, repeatDay);
        this.isActive = true;
    }

    private LocalDate calculateInitialNextDate(LocalDate startDate, Integer repeatDay) {

        if (repeatDay < 1 || repeatDay > 31) {
            throw new CustomException(AssetErrorCode.INVALID_REPEAT_DAY);
        }

        int year = startDate.getYear();
        int month = startDate.getMonthValue();
        int lastDayOfMonth = startDate.lengthOfMonth();
        int validDay = Math.min(repeatDay, lastDayOfMonth);
        LocalDate targetDate = LocalDate.of(year, month, validDay);

        if (startDate.isAfter(targetDate)) {
            targetDate = targetDate.plusMonths(1).withDayOfMonth(Math.min(repeatDay, targetDate.plusMonths(1).lengthOfMonth()));
        }

        return targetDate;
    }
}
