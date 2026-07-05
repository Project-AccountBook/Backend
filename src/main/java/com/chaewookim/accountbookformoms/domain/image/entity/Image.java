package com.chaewookim.accountbookformoms.domain.image.entity;

import com.chaewookim.accountbookformoms.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "image",
        indexes = @Index(name = "idx_image_reference", columnList = "reference_id, reference_type")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Image extends BaseEntity {

    public enum ReferenceType {BOARD, GROUP_PURCHASE}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "s3_key", length = 500)
    private String s3Key;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 20)
    private ReferenceType referenceType;

    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Builder
    public Image(String imageUrl, String s3Key, ReferenceType referenceType, Long referenceId, int sortOrder) {
        this.imageUrl = imageUrl;
        this.s3Key = s3Key;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.sortOrder = sortOrder;
    }
}
