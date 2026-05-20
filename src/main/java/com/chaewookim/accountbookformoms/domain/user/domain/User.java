package com.chaewookim.accountbookformoms.domain.user.domain;

import com.chaewookim.accountbookformoms.domain.user.dto.request.UpdateRequest;
import com.chaewookim.accountbookformoms.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.Valid;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@SQLDelete(sql = "UPDATE user SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false,  unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean isAdmin = false;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.USER;

    @Column(nullable = false)
    private String address;

    @Column
    private LocalDateTime deletedAt;

    @Builder
    public User(String username, String email, String password, LocalDate birthDate, String address) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.birthDate = birthDate;
        this.address = address;
    }

    public User updateUser(@Valid UpdateRequest request) {

        this.username = request.username();
        this.email = request.email();
        this.address = request.address();

        return this;
    }

    @Builder(builderMethodName = "forTestBuilder")
    public User(Long id, String username, String email, String password, boolean isAdmin, String address) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.isAdmin = isAdmin;
        this.address = address;
    }
}
