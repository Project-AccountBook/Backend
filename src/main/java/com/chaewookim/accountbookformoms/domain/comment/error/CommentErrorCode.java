package com.chaewookim.accountbookformoms.domain.comment.error;

import com.chaewookim.accountbookformoms.global.error.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommentErrorCode implements BaseErrorCode {

    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 댓글을 찾을 수 없습니다."),
    COMMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "본인의 댓글만 수정/삭제할 수 있습니다."),
    COMMENT_REPLY_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "대댓글에는 답글을 달 수 없습니다."),
    COMMENT_REFERENCE_MISMATCH(HttpStatus.BAD_REQUEST, "부모 댓글의 게시물 정보와 일치하지 않습니다.");

    private final HttpStatus status;
    private final String message;
}
