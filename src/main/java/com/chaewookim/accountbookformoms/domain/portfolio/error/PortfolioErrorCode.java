package com.chaewookim.accountbookformoms.domain.portfolio.error;

import com.chaewookim.accountbookformoms.global.error.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PortfolioErrorCode implements BaseErrorCode {

    INVALID_YEAR_MONTH(HttpStatus.BAD_REQUEST, "연/월 값이 올바르지 않습니다."),
    INVALID_AMOUNT_RANGE(HttpStatus.BAD_REQUEST, "금액 구간이 올바르지 않습니다."),
    CANNOT_COMPARE_SELF(HttpStatus.BAD_REQUEST, "자기 자신과는 비교할 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
