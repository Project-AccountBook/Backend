package com.chaewookim.accountbookformoms.domain.asset.error;

import com.chaewookim.accountbookformoms.global.error.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AssetErrorCode implements BaseErrorCode {

    // Account
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "계좌를 찾을 수 없습니다."),

    // FixedTransaction
    INVALID_REPEAT_DAY(HttpStatus.BAD_REQUEST, "반복 일자는 1~31일 사이여야 합니다.");

    private final HttpStatus status;
    private final String message;
}
