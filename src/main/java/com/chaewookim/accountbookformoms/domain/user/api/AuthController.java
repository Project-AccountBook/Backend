package com.chaewookim.accountbookformoms.domain.user.api;

import com.chaewookim.accountbookformoms.domain.user.application.AuthService;
import com.chaewookim.accountbookformoms.domain.user.dto.request.LoginRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.request.LogoutRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.request.TokenReissueRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.response.LoginResponse;
import com.chaewookim.accountbookformoms.domain.user.dto.response.TokenResponse;
import com.chaewookim.accountbookformoms.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @Operation(summary = "로그인", description = "username과 비밀번호로 로그인하고 토큰 발급")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest request) {

        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @Operation(summary = "로그아웃", description = "로그아웃 진행")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@RequestBody @Valid LogoutRequest request) {

        authService.logout(request);

        return ResponseEntity.ok(ApiResponse.success("로그아웃 성공"));
    }

    @Operation(summary = "토큰 재발급", description = "RefreshToken을 이용해 AccessToken 재발급")
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@RequestBody @Valid TokenReissueRequest request) {

        return ResponseEntity.ok(ApiResponse.success(authService.reissue(request)));
    }
}
