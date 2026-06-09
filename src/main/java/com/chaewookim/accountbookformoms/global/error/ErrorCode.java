package com.chaewookim.accountbookformoms.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements BaseErrorCode {

    // 400 Bad Request
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "파라미터 값을 확인해주세요."),

    // 401 Unauthorized
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),

    // 404 Not Found
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 에러입니다."),

    // 계좌 존재하지 않음
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 계좌가 존재하지 않습니다."),

    // 접근 권한 부족
    ACCOUNT_ACCESS_DENIED(HttpStatus.UNAUTHORIZED, "본인의 계좌만 삭제할 수 있습니다."),

    // 해당 트랜잭션 찾지 못 함
    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 내역을 찾을 수 없거나 접근 권한이 없습니다."),

    // 고정 트랜잭션 존재하지 않음
    NO_FIXED_TRANSACTIONS(HttpStatus.NOT_FOUND, "고정수입 또는 고정지출을 찾을 수 없거나 접근 권한이 없습니다."),

    // 계좌 찾을 수 없음
    ASSET_NOT_FOUND(HttpStatus.NOT_FOUND, "계좌를 찾을 수 없거나 접근 권한이 없습니다."),

    // 금액은 NULL이 될 수 없음
    AMOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "금액은 비어있을 수 없습니다."),

    // 공동구매 관련 에러
    GROUP_PURCHASE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 공동구매를 찾을 수 없습니다."),
    UNAUTHORIZED_GROUP_PURCHASE(HttpStatus.FORBIDDEN, "해당 공동구매에 대한 권한이 없습니다."),

    // 공동구매 신청 관련 에러
    APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 공동구매 신청을 찾을 수 없습니다."),
    UNAUTHORIZED_APPLICATION(HttpStatus.FORBIDDEN, "해당 신청에 대한 권한이 없습니다."),

    // 상품 관련 에러
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 상품을 찾을 수 없습니다."),

    // 카테고리 관련 에러
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 카테고리를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
