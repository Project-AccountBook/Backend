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
    CATEGORY_ID_REQUIRED(HttpStatus.BAD_REQUEST, "카테고리 비교를 위해 카테고리 ID가 필요합니다."),
    RADIUS_REQUIRED(HttpStatus.BAD_REQUEST, "위치 비교를 위해 반경(km)이 필요합니다."),
    INVALID_RADIUS(HttpStatus.BAD_REQUEST, "반경은 0.1km 이상 50km 이하여야 합니다."),
    LOCATION_PAIR_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "위치 기반 1:1 비교는 지원하지 않습니다."),
    CANNOT_COMPARE_SELF(HttpStatus.BAD_REQUEST, "자기 자신과는 비교할 수 없습니다."),
    TARGET_NOT_PUBLIC(HttpStatus.FORBIDDEN, "비공개 사용자의 예산은 조회할 수 없습니다."),
    BUDGET_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 해당 월/카테고리에 설정된 예산이 존재합니다."),
    BUDGET_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 예산을 찾을 수 없습니다."),
    BUDGET_FORBIDDEN(HttpStatus.FORBIDDEN, "해당 예산에 대한 수정/삭제 권한이 없습니다.");

    private final HttpStatus status;
    private final String message;
}
