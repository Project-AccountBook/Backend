package com.chaewookim.accountbookformoms.domain.user.dto.response;

import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.domain.user.enums.UserRole;

import java.time.LocalDate;

public record UserResponse(
        Long id,
        String email,
        String username,
        String address,
        LocalDate birthDate,
        UserRole role
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getAddress(),
                user.getBirthDate(),
                user.getRole()
        );
    }
}
