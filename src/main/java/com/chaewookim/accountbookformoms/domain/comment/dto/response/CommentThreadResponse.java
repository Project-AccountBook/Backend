package com.chaewookim.accountbookformoms.domain.comment.dto.response;

import java.util.List;

public record CommentThreadResponse(
        CommentResponse parent,
        List<CommentResponse> replies
) {
}
