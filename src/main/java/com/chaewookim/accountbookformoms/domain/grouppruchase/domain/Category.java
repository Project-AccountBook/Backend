package com.chaewookim.accountbookformoms.domain.grouppruchase.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

    private Long id;
    private String name;       // 카테고리명(밀키트, 대용량 식자재 등)
    private int sortOrder;     // 정렬

    @Builder
    public Category(Long id, String name, int sortOrder) {
        this.id = id;
        this.name = name;
        this.sortOrder = sortOrder;
    }
}