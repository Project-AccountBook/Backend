package com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response;

import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.Product;

import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        long price,
        String description,
        String imageUrl,
        Long categoryId,
        Long applicationId,
        Long reportId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getDescription(),
                product.getImageUrl(),
                product.getCategoryId(),
                product.getApplicationId(),
                product.getReportId(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
