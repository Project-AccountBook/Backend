package com.chaewookim.accountbookformoms.domain.budget.error;

import com.chaewookim.accountbookformoms.global.error.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BudgetErrorCode implements BaseErrorCode {

    INVALID_YEAR_MONTH(HttpStatus.BAD_REQUEST, "연/월 값이 올바르지 않습니다."),
    INVALID_AMOUNT_RANGE(HttpStatus.BAD_REQUEST, "금액 구간이 올바르지 않습니다."),
    BIRTH_DATE_REQUIRED(HttpStatus.BAD_REQUEST, "나이대 비교를 위해 생년월일이 필요합니다."),
    CATEGORY_ID_REQUIRED(HttpStatus.BAD_REQUEST, "카테고리 비교를 위해 카테고리 ID가 필요합니다.");

    private final HttpStatus status;
    private final String message;
}
