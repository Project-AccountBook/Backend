package com.chaewookim.accountbookformoms.domain.user.entity;

import com.chaewookim.accountbookformoms.global.entity.BaseEntity;
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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE interest_category SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class InterestCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String coopCategoryName;

    @Column(nullable = false)
    private boolean isAlarmEnabled;

    @Builder
    public InterestCategory(User user, String coopCategoryName, boolean isAlarmEnabled) {
        this.user = user;
        this.coopCategoryName = coopCategoryName;
        this.isAlarmEnabled = isAlarmEnabled;
    }

    public void updateAlarmStatus(boolean isAlarmEnabled) {
        this.isAlarmEnabled = isAlarmEnabled;
    }
}
