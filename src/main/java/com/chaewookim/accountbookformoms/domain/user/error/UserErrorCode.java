package com.chaewookim.accountbookformoms.domain.user.error;

import com.chaewookim.accountbookformoms.global.error.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements BaseErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 올바르지 않습니다."),
    PASSWORD_NOT_MATCH(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
    UNSUPPORTED_SOCIAL_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 로그인 방식입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    INVALID_COORDINATES(HttpStatus.BAD_REQUEST, "위경도 좌표가 유효하지 않습니다."),
    LOCATION_NOT_REGISTERED(HttpStatus.CONFLICT, "등록된 위치 정보가 없습니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.UNAUTHORIZED, "이메일 인증이 완료되지 않았습니다."),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "인증 번호가 올바르지 않거나 만료되었습니다."),
    VERIFICATION_ATTEMPTS_EXCEEDED(HttpStatus.BAD_REQUEST, "인증 횟수를 초과했습니다. 인증번호를 다시 받아주세요."),
    INVALID_OAUTH_CODE(HttpStatus.UNAUTHORIZED, "소셜 로그인 인가 코드가 올바르지 않거나 만료되었습니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "너무 많은 요청입니다. 잠시 후 다시 시도해주세요."),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이메일 전송에 실패했습니다. 잠시 후 다시 시도해주세요."),
    ALREADY_EXIST_CATEGORY(HttpStatus.CONFLICT, "이미 등록된 관심 카테고리입니다.");

    private final HttpStatus status;
    private final String message;
}
