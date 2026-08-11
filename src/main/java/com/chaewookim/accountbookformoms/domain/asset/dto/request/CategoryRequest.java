package com.chaewookim.accountbookformoms.domain.asset.dto.request;

import com.chaewookim.accountbookformoms.domain.asset.entity.TransactionCategory;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoryRequest(

        @NotBlank(message = "카테고리 이름은 필수입니다.")
        String name,

        @NotNull(message = "카테고리 타입은 필수입니다.")
        TransactionType type
) {
    public TransactionCategory toEntity(User user) {
        return TransactionCategory.builder()
                .user(user)
                .name(this.name)
                .type(this.type)
                .build();
    }
}
