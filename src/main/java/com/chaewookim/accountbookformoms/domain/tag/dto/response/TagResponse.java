package com.chaewookim.accountbookformoms.domain.tag.dto.response;

import com.chaewookim.accountbookformoms.domain.tag.entity.Tag;

public record TagResponse(
        Long id,
        String name
) {
    public static TagResponse from(Tag t) {
        return new TagResponse(t.getId(), t.getName());
    }
}
