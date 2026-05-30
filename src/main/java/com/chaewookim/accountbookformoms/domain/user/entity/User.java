package com.chaewookim.accountbookformoms.domain.user.entity;

import com.chaewookim.accountbookformoms.domain.user.enums.SocialProvider;
import com.chaewookim.accountbookformoms.domain.user.enums.UserRole;
import com.chaewookim.accountbookformoms.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
@SQLDelete(sql = "UPDATE user SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,  unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SocialProvider provider;

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

//    public User updateUser(@Valid UpdateRequest request) {
//        this.username = request.username();
//        this.email = request.email();
//        this.address = request.address();
//        return this;
//    }

    @Builder(builderMethodName = "forTestBuilder")
    public User(Long id, String email, String password, String username, LocalDate birthDate, String address, UserRole role, SocialProvider provider) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.username = username;
        this.birthDate = birthDate;
        this.address = address;
        this.role = (role != null) ? role : UserRole.ROLE_USER;
        this.provider = (provider != null) ? provider : SocialProvider.LOCAL;
    }
}
