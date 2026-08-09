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
    INVALID_ACCOUNT_BALANCE(HttpStatus.BAD_REQUEST, "자산 계좌의 잔액은 0원 이상이어야 합니다."),
    INVALID_LIABILITY_BALANCE(HttpStatus.BAD_REQUEST, "신용카드와 대출 잔액은 0원을 초과할 수 없습니다."),
    INVALID_CREDIT_LIMIT(HttpStatus.BAD_REQUEST, "신용 한도는 0원 이상이어야 합니다."),
    CREDIT_LIMIT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "신용 한도는 신용카드 계좌에만 설정할 수 있습니다."),
    CREDIT_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "신용카드 한도를 초과했습니다."),
    INVALID_LOAN_LIMIT(HttpStatus.BAD_REQUEST, "대출 약정 금액은 0원 이상이어야 합니다."),
    INVALID_DISBURSED_AMOUNT(HttpStatus.BAD_REQUEST, "누적 대출 실행액은 0원 이상이며 약정 금액을 초과할 수 없습니다."),
    INVALID_UNDISBURSED_LOAN_BALANCE(HttpStatus.BAD_REQUEST, "미실행 대출의 초기 잔액은 0원이어야 합니다."),
    LOAN_FIELDS_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "대출 약정 정보는 대출 계좌에만 설정할 수 있습니다."),
    LOAN_DEBT_EXCEEDS_DISBURSED_AMOUNT(HttpStatus.BAD_REQUEST, "현재 대출 원금은 누적 실행액을 초과할 수 없습니다."),
    LOAN_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "대출 누적 실행액이 약정 금액을 초과합니다."),
    INVALID_LOAN_REVERSAL(HttpStatus.BAD_REQUEST, "후속 상환 내역 때문에 대출 실행 거래를 되돌릴 수 없습니다."),
    INVALID_GOAL_AMOUNT(HttpStatus.BAD_REQUEST, "목표 금액은 0보다 커야 합니다."),
    INVALID_DEBT_GOAL_AMOUNT(HttpStatus.BAD_REQUEST, "대출 목표 잔액은 0원 이상이어야 합니다."),
    GOAL_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "신용카드는 목표를 설정할 수 없습니다."),

    // Transaction
    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "거래 내역을 찾을 수 없습니다."),
    TRANSACTION_FORBIDDEN(HttpStatus.FORBIDDEN, "본인의 거래 내역만 접근할 수 있습니다."),
    TRANSFER_TO_SELF_FORBIDDEN(HttpStatus.BAD_REQUEST, "자기 자신에게는 이체할 수 없습니다."),
    TARGET_ACCOUNT_REQUIRED(HttpStatus.BAD_REQUEST, "이체 시 입금 대상 계좌는 필수입니다."),
    INCOME_NOT_ALLOWED_FOR_LIABILITY(HttpStatus.BAD_REQUEST, "신용카드와 대출에는 수입을 직접 등록할 수 없습니다. 자산 계좌에서 이체해 주세요."),
    NORMAL_TRANSACTION_NOT_ALLOWED_FOR_LOAN(HttpStatus.BAD_REQUEST, "대출 계좌에는 일반 수입·지출을 등록할 수 없습니다."),
    INVALID_LOAN_TRANSFER(HttpStatus.BAD_REQUEST, "대출 실행은 대출 계좌에서 자산 계좌로, 원금 상환은 자산 계좌에서 대출 계좌로만 이체할 수 있습니다."),
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
    DUPLICATE_CATEGORY_NAME(HttpStatus.CONFLICT, "이미 존재하는 카테고리 이름입니다.");

    private final HttpStatus status;
    private final String message;
}
