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
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "잔액이 부족합니다."),

    // Transaction
    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "거래 내역을 찾을 수 없습니다."),
    TRANSACTION_FORBIDDEN(HttpStatus.FORBIDDEN, "본인의 거래 내역만 접근할 수 있습니다."),

    // FixedTransaction
    INVALID_REPEAT_DAY(HttpStatus.BAD_REQUEST, "반복일은 1일부터 31일 사이여야 합니다."),
    FIXED_TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 고정 내역을 찾을 수 없습니다."),
    FIXED_TRANSACTION_FORBIDDEN(HttpStatus.FORBIDDEN, "본인의 고정 내역만 접근할 수 있습니다."),

    // Category
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
    CATEGORY_FORBIDDEN(HttpStatus.FORBIDDEN, "본인의 카테고리만 수정/삭제 가능합니다."),
    CATEGORY_IMMUTABLE(HttpStatus.BAD_REQUEST, "기본 카테고리는 수정하거나 삭제할 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
