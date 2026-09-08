package com.chaewookim.accountbookformoms.domain.board.error;

import com.chaewookim.accountbookformoms.global.error.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BoardErrorCode implements BaseErrorCode {

    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 게시물을 찾을 수 없습니다."),
    BOARD_ACCESS_DENIED(HttpStatus.FORBIDDEN, "본인의 게시물만 수정/삭제할 수 있습니다.");

    private final HttpStatus status;
    private final String message;
}
