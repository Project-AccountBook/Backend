package com.chaewookim.accountbookformoms.domain.user.entity;

import com.chaewookim.accountbookformoms.domain.user.enums.SocialProvider;
import com.chaewookim.accountbookformoms.domain.user.enums.UserRole;
import com.chaewookim.accountbookformoms.global.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE user SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,  unique = true)
    private String email;

    private String password;

    @Column(nullable = false)
    private String username;

    private LocalDate birthDate;

    private String address;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SocialProvider provider;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserSetting userSetting;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserNotificationSetting userNotificationSetting;

    @Builder
    public User(String email, String password, String username, LocalDate birthDate, String address, UserRole role, SocialProvider provider) {
        this.email = email;
        this.password = password;
        this.username = username;
        this.birthDate = birthDate;
        this.address = address;
        this.role = (role != null) ? role : UserRole.ROLE_USER;
        this.provider = (provider != null) ? provider : SocialProvider.LOCAL;
    }

    public void setSettings(UserSetting userSetting, UserNotificationSetting notificationSetting) {
        this.userSetting = userSetting;
        this.userNotificationSetting = notificationSetting;
    }

    public User update(String username) {
        this.username = username;
        return this;
    }

    public void updateProfile(String username, LocalDate birthDate, String address) {
        this.username = username;
        this.birthDate = birthDate;
        this.address = address;
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public void restore(String username, String password, LocalDate birthDate, String address) {
        super.restore();
        this.password = password;
        this.username = username;
        this.birthDate = birthDate;
        this.address = address;
    }
}
