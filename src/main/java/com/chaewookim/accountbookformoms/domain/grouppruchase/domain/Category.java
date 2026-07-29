package com.chaewookim.accountbookformoms.domain.grouppruchase.domain;

import com.chaewookim.accountbookformoms.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "group_purchase_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE group_purchase_category SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;       // 카테고리명(밀키트, 대용량 식자재 등)

    @Column(nullable = false)
    private int sortOrder;     // 정렬 순서

    @Column(columnDefinition = "TEXT")
    private String description; // 카테고리 설명

    @Builder
    public Category(Long id, String name, int sortOrder, String description) {
        this.id = id;
        this.name = name;
        this.sortOrder = sortOrder;
        this.description = description;
    }

    public void update(String name, int sortOrder, String description) {
        this.name = name;
        this.sortOrder = sortOrder;
        this.description = description;
    }
}