package com.chaewookim.accountbookformoms.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserNotificationSetting {

    @Id
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private Boolean isBudgetAlertEnabled;       // 예산 알림

    @Column(nullable = false)
    private Boolean isInterestCategoryEnabled;  // 관심 카테고리 알림

    @Column(nullable = false)
    private Boolean isGoalAlertEnabled;         // 계좌 목표 달성 알림

    @Column(nullable = false)
    private Boolean isSystemAlertEnabled;       // 시스템 알림

    @Builder
    public UserNotificationSetting(User user, Boolean isBudgetAlertEnabled, Boolean isInterestCategoryEnabled,
                                   Boolean isGoalAlertEnabled, Boolean isSystemAlertEnabled) {
        this.user = user;
        this.isBudgetAlertEnabled = (isBudgetAlertEnabled != null) ? isBudgetAlertEnabled : true;
        this.isInterestCategoryEnabled = (isInterestCategoryEnabled != null) ? isInterestCategoryEnabled : true;
        this.isGoalAlertEnabled = (isGoalAlertEnabled != null) ? isGoalAlertEnabled : true;
        this.isSystemAlertEnabled = (isSystemAlertEnabled != null) ? isSystemAlertEnabled : true;
    }

    public void updateNotificationSettings(Boolean isBudgetAlertEnabled, Boolean isInterestCategoryEnabled,
                                           Boolean isGoalAlertEnabled, Boolean isSystemAlertEnabled) {
        if (isBudgetAlertEnabled != null) this.isBudgetAlertEnabled = isBudgetAlertEnabled;
        if (isInterestCategoryEnabled != null) this.isInterestCategoryEnabled = isInterestCategoryEnabled;
        if (isGoalAlertEnabled != null) this.isGoalAlertEnabled = isGoalAlertEnabled;
        if (isSystemAlertEnabled != null) this.isSystemAlertEnabled = isSystemAlertEnabled;
    }

    public boolean isGoalAlertEnabledOrDefault() {
        return !Boolean.FALSE.equals(isGoalAlertEnabled);
    }

    public void resetToDefaults() {
        this.isBudgetAlertEnabled = true;
        this.isInterestCategoryEnabled = true;
        this.isGoalAlertEnabled = true;
        this.isSystemAlertEnabled = true;
    }
}
