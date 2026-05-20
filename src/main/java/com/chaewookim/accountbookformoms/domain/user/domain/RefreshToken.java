package com.chaewookim.accountbookformoms.domain.user.domain;

import com.chaewookim.accountbookformoms.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Getter
@NoArgsConstructor
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String token;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Date expireTime;

    @Builder
    public RefreshToken(String token, Long userId, Date expireTime) {
        this.token = token;
        this.userId = userId;
        this.expireTime = expireTime;
    }

    public void updateToken(String newRefreshToken) {

        this.token = newRefreshToken;
    }

    @Builder(builderMethodName = "forTestBuilder")
    public RefreshToken(String token, Long userId) {
        this.token = token;
        this.userId = userId;
    }
}
