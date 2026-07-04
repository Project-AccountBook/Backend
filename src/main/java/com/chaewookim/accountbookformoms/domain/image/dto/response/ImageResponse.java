package com.chaewookim.accountbookformoms.domain.image.dto.response;

import com.chaewookim.accountbookformoms.domain.image.entity.Image;

public record ImageResponse(
        Long id,
        String imageUrl,
        int sortOrder
) {
    public static ImageResponse from(Image image) {
        return new ImageResponse(image.getId(), image.getImageUrl(), image.getSortOrder());
    }
}
