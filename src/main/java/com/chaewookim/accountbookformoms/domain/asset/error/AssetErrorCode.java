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
    DUPLICATE_ACCOUNT_NAME(HttpStatus.CONFLICT, "이미 존재하는 계좌 이름입니다."),
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "잔액이 부족합니다."),
    INVALID_GOAL_AMOUNT(HttpStatus.BAD_REQUEST, "목표 금액은 0보다 커야 합니다."),

    // Transaction
    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "거래 내역을 찾을 수 없습니다."),
    TRANSACTION_FORBIDDEN(HttpStatus.FORBIDDEN, "본인의 거래 내역만 접근할 수 있습니다."),
    TRANSFER_TO_SELF_FORBIDDEN(HttpStatus.BAD_REQUEST, "자기 자신에게는 이체할 수 없습니다."),
    TARGET_ACCOUNT_REQUIRED(HttpStatus.BAD_REQUEST, "이체 시 입금 대상 계좌는 필수입니다."),
    INVALID_EXPORT_DATE_RANGE(HttpStatus.BAD_REQUEST, "내보내기 종료일은 시작일 이후여야 합니다."),
    EXPORT_PERIOD_TOO_LONG(HttpStatus.BAD_REQUEST, "거래 내역 내보내기는 최대 12개월까지 가능합니다."),

    // FixedTransaction
    INVALID_REPEAT_DAY(HttpStatus.BAD_REQUEST, "반복일 값이 올바르지 않습니다. (매주: 1~7, 매월/매년: 1~31)"),
    INVALID_REPEAT_MONTH(HttpStatus.BAD_REQUEST, "반복 월은 1월부터 12월 사이여야 합니다."),
    FIXED_TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 고정 내역을 찾을 수 없습니다."),
    FIXED_TRANSACTION_FORBIDDEN(HttpStatus.FORBIDDEN, "본인의 고정 내역만 접근할 수 있습니다."),

    // Category
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
    CATEGORY_FORBIDDEN(HttpStatus.FORBIDDEN, "본인의 카테고리만 수정/삭제 가능합니다."),
    CATEGORY_IMMUTABLE(HttpStatus.BAD_REQUEST, "기본 카테고리는 수정하거나 삭제할 수 없습니다."),
    CATEGORY_IN_USE(HttpStatus.BAD_REQUEST, "이 카테고리를 사용 중인 거래·고정내역이 있어 삭제할 수 없습니다."),
    DUPLICATE_CATEGORY_NAME(HttpStatus.CONFLICT, "이미 존재하는 카테고리 이름입니다."),
    CATEGORY_ALLOCATION_ONLY_TRANSFER(HttpStatus.BAD_REQUEST, "이체 카테고리만 저축률·투자율 설정이 가능합니다.");

    private final HttpStatus status;
    private final String message;
}
