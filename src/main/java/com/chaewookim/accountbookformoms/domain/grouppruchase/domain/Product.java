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
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE product SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int price;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = true)
    private String imageUrl;

    @Column(nullable = false)
    private Long categoryId;

    @Column(nullable = true)
    private Long applicationId;  // 참고한 공동구매 신청 ID (선택)

    @Column(nullable = true)
    private Long reportId;       // 참고한 제보 ID (선택)

    @Builder
    public Product(Long id, String name, int price, String description, String imageUrl, Long categoryId, Long applicationId, Long reportId) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.description = description;
        this.imageUrl = imageUrl;
        this.categoryId = categoryId;
        this.applicationId = applicationId;
        this.reportId = reportId;
    }

    public void update(String name, int price, String description, String imageUrl, Long categoryId, Long applicationId, Long reportId) {
        this.name = name;
        this.price = price;
        this.description = description;
        this.imageUrl = imageUrl;
        this.categoryId = categoryId;
        this.applicationId = applicationId;
        this.reportId = reportId;
    }
}
