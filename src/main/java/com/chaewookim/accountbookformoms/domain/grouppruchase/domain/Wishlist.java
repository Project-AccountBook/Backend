package com.chaewookim.accountbookformoms.domain.grouppruchase.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wishlist {

    private Long id;
    private Long userId;
    private Long groupPurchaseId;
    private LocalDateTime createdAt;

    @Builder
    public Wishlist(Long id, Long userId, Long groupPurchaseId) {
        this.id = id;
        this.userId = userId;
        this.groupPurchaseId = groupPurchaseId;
        this.createdAt = LocalDateTime.now();
    }
}