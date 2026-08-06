package com.chaewookim.accountbookformoms.domain.user.api;

import com.chaewookim.accountbookformoms.domain.user.application.AuthService;
import com.chaewookim.accountbookformoms.domain.user.application.EmailVerificationService;
import com.chaewookim.accountbookformoms.domain.user.dto.request.EmailRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.request.LoginRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.request.ReissueRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.request.ResetPasswordRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.request.VerifyRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.response.TokenResponse;
import com.chaewookim.accountbookformoms.global.common.ApiResponse;
import com.chaewookim.accountbookformoms.global.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.error.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증(Auth)", description = "로그인/로그아웃/토큰 재발급 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(14 * 24 * 60 * 60)
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하고 토큰 발급")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletResponse response
    ) {
        TokenResponse tokenResponse = authService.login(request);
        setRefreshTokenCookie(response, tokenResponse.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(tokenResponse));
    }

    @Operation(summary = "토큰 재발급", description = "RefreshToken을 이용해 AccessToken 재발급")
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        ReissueRequest request = new ReissueRequest(refreshToken);
        TokenResponse tokenResponse = authService.reissue(request);
        setRefreshTokenCookie(response, tokenResponse.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(tokenResponse));
    }

    @Operation(summary = "회원가입용 인증번호 발송", description = "회원가입을 위한 이메일 인증")
    @PostMapping("/email/send/signup")
    public ResponseEntity<ApiResponse<Void>> sendSignupCode(
            @RequestBody @Valid EmailRequest request
    ) {
        authService.sendSignupVerificationCode(request.email());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "비밀번호 재설정용 인증번호 발송", description = "비밀번호 찾기를 위한 이메일 인증")
    @PostMapping("/email/send/password")
    public ResponseEntity<ApiResponse<Void>> sendPasswordCode(
            @RequestBody @Valid EmailRequest request
    ) {
        authService.requestPasswordReset(request.email());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "인증번호 검증", description = "회원가입/비밀번호 재설정 인증번호 확인")
    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<Boolean>> verifyCode(
            @RequestBody VerifyRequest request
    ) {
        boolean isSuccess = emailVerificationService.verifyCode(request.email(), request.code(), request.type());
        return ResponseEntity.ok(ApiResponse.success(isSuccess));
    }

    @Operation(summary = "비밀번호 재설정", description = "인증번호 검증 후 비밀번호 변경")
    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestBody @Valid ResetPasswordRequest request
    ) {
        authService.resetPassword(request.email(), request.code(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "로그아웃", description = "토큰 삭제 후 로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletResponse response
    ) {
        authService.logout(principal.getUsername());
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
