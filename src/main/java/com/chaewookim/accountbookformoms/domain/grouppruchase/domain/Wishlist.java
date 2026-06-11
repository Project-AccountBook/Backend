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
@Table(
        name = "wishlist",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_wishlist_user_group_purchase",
                        columnNames = {"user_id", "group_purchase_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE wishlist SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Wishlist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "group_purchase_id", nullable = false)
    private Long groupPurchaseId;

    @Builder
    public Wishlist(Long id, Long userId, Long groupPurchaseId) {
        this.id = id;
        this.userId = userId;
        this.groupPurchaseId = groupPurchaseId;
    }
}