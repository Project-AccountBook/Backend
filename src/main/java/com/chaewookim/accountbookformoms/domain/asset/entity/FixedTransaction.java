package com.chaewookim.accountbookformoms.domain.asset.entity;

import com.chaewookim.accountbookformoms.domain.asset.dto.request.FixedTransactionRequest;
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
import java.time.YearMonth;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE fixed_transaction SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Table(name = "fixed_transaction",
        indexes = {
                @Index(name = "idx_fixed_transaction_user", columnList = "user_id")
        })
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
    @Enumerated(EnumType.STRING)
    private TransactionFrequency frequency;

    /** WEEKLY: 1(월)~7(일), MONTHLY/YEARLY: 1~31일 */
    @Column(nullable = false)
    private Integer repeatDay;

    /** YEARLY 전용: 1~12월 */
    private Integer repeatMonth;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDate lastExecutedDate;

    private LocalDate nextExecutionDate;

    private String description;

    @Column(nullable = false)
    private Boolean isActive;

    @Builder
    public FixedTransaction(User user, Account account, Account targetAccount, TransactionCategory transactionCategory,
                            TransactionType type, BigDecimal amount,
                            TransactionFrequency frequency, Integer repeatDay, Integer repeatMonth,
                            LocalDate startDate, LocalDate endDate, String description) {
        this.user = user;
        this.account = account;
        this.targetAccount = targetAccount;
        this.transactionCategory = transactionCategory;
        this.type = type;
        this.amount = amount;
        this.frequency = frequency;
        this.repeatDay = repeatDay;
        this.repeatMonth = repeatMonth;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
        this.nextExecutionDate = calculateInitialNextDate(startDate, frequency, repeatDay, repeatMonth);
        this.isActive = true;
    }

    public static void validateSchedule(TransactionFrequency frequency, Integer repeatDay, Integer repeatMonth) {
        switch (frequency) {
            case WEEKLY -> {
                if (repeatDay == null || repeatDay < 1 || repeatDay > 7) {
                    throw new CustomException(AssetErrorCode.INVALID_REPEAT_DAY);
                }
            }
            case MONTHLY -> {
                if (repeatDay == null || repeatDay < 1 || repeatDay > 31) {
                    throw new CustomException(AssetErrorCode.INVALID_REPEAT_DAY);
                }
            }
            case YEARLY -> {
                if (repeatMonth == null || repeatMonth < 1 || repeatMonth > 12) {
                    throw new CustomException(AssetErrorCode.INVALID_REPEAT_MONTH);
                }
                if (repeatDay == null || repeatDay < 1 || repeatDay > 31) {
                    throw new CustomException(AssetErrorCode.INVALID_REPEAT_DAY);
                }
            }
        }
    }

    private static int effectiveDayOfMonth(int repeatDay, int year, int month) {
        return Math.min(repeatDay, YearMonth.of(year, month).lengthOfMonth());
    }

    private static LocalDate calculateInitialNextDate(LocalDate startDate, TransactionFrequency frequency,
                                                      Integer repeatDay, Integer repeatMonth) {
        validateSchedule(frequency, repeatDay, repeatMonth);

        return switch (frequency) {
            case WEEKLY -> {
                LocalDate date = startDate;
                while (date.getDayOfWeek().getValue() != repeatDay) {
                    date = date.plusDays(1);
                }
                yield date;
            }
            case MONTHLY -> {
                int year = startDate.getYear();
                int month = startDate.getMonthValue();
                int day = effectiveDayOfMonth(repeatDay, year, month);
                LocalDate target = LocalDate.of(year, month, day);
                if (startDate.isAfter(target)) {
                    LocalDate nextMonth = startDate.plusMonths(1);
                    day = effectiveDayOfMonth(repeatDay, nextMonth.getYear(), nextMonth.getMonthValue());
                    target = LocalDate.of(nextMonth.getYear(), nextMonth.getMonthValue(), day);
                }
                yield target;
            }
            case YEARLY -> {
                int year = startDate.getYear();
                int day = effectiveDayOfMonth(repeatDay, year, repeatMonth);
                LocalDate target = LocalDate.of(year, repeatMonth, day);
                if (startDate.isAfter(target)) {
                    year++;
                    day = effectiveDayOfMonth(repeatDay, year, repeatMonth);
                    target = LocalDate.of(year, repeatMonth, day);
                }
                yield target;
            }
        };
    }

    public boolean isExecutionDay(LocalDate today) {
        if (endDate != null && today.isAfter(endDate)) {
            return false;
        }
        if (today.isBefore(startDate)) {
            return false;
        }

        return switch (frequency) {
            case WEEKLY -> today.getDayOfWeek().getValue() == repeatDay;
            case MONTHLY -> today.getDayOfMonth() == effectiveDayOfMonth(repeatDay, today.getYear(), today.getMonthValue());
            case YEARLY -> today.getMonthValue() == repeatMonth
                    && today.getDayOfMonth() == effectiveDayOfMonth(repeatDay, today.getYear(), repeatMonth);
        };
    }

    public void update(Account account, Account targetAccount, TransactionCategory category, FixedTransactionRequest request) {
        this.account = account;
        this.targetAccount = targetAccount;
        this.transactionCategory = category;
        this.type = request.type();
        this.amount = request.amount();
        this.frequency = request.frequency();
        this.repeatDay = request.repeatDay();
        this.repeatMonth = request.repeatMonth();
        this.startDate = request.startDate();
        this.endDate = request.endDate();
        this.description = request.description();

        LocalDate basis = LocalDate.now().isBefore(startDate) ? startDate : LocalDate.now();
        this.nextExecutionDate = calculateInitialNextDate(basis, frequency, repeatDay, repeatMonth);
    }

    public void toggleActive() {
        this.isActive = !this.isActive;
    }

    private LocalDate calculateNextExecutionDate(LocalDate executedDate) {
        return switch (frequency) {
            case WEEKLY -> executedDate.plusWeeks(1);
            case MONTHLY -> {
                LocalDate next = executedDate.plusMonths(1);
                int day = effectiveDayOfMonth(repeatDay, next.getYear(), next.getMonthValue());
                yield LocalDate.of(next.getYear(), next.getMonthValue(), day);
            }
            case YEARLY -> {
                int year = executedDate.getYear() + 1;
                int day = effectiveDayOfMonth(repeatDay, year, repeatMonth);
                yield LocalDate.of(year, repeatMonth, day);
            }
        };
    }

    public void updateExecutionStatus(LocalDate executedDate) {
        this.lastExecutedDate = executedDate;
        this.nextExecutionDate = calculateNextExecutionDate(executedDate);
    }

    public void alignNextExecutionDateIfStale(LocalDate today) {
        if (isExecutionDay(today) && nextExecutionDate != null && nextExecutionDate.isAfter(today)) {
            this.nextExecutionDate = today;
        }
    }
}
