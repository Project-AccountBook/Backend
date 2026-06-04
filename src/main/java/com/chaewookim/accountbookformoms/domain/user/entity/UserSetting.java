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
public class UserSetting {

    @Id
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private Integer budgetAlertThreshold;   // 예산 대비 지출 알림 기준 조건

    @Column(nullable = false)
    private Boolean isPortfolioPublic;

    @Builder
    public UserSetting(User user, Integer budgetAlertThreshold, Boolean isPortfolioPublic) {
        this.user = user;
        this.budgetAlertThreshold = (budgetAlertThreshold != null) ? budgetAlertThreshold : 80;
        this.isPortfolioPublic = (isPortfolioPublic != null) ? isPortfolioPublic : false;
    }

    public void updateSettings(Integer budgetAlertThreshold, Boolean isPortfolioPublic) {
        if (budgetAlertThreshold != null) this.budgetAlertThreshold = budgetAlertThreshold;
        if (isPortfolioPublic != null) this.isPortfolioPublic = isPortfolioPublic;
    }
}
