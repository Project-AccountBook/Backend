package com.chaewookim.accountbookformoms.domain.user.application;

import com.chaewookim.accountbookformoms.domain.user.dao.RefreshTokenRepository;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.domain.RefreshToken;
import com.chaewookim.accountbookformoms.domain.user.domain.User;
import com.chaewookim.accountbookformoms.domain.user.dto.request.LogoutRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.request.TokenReissueRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.request.UpdateRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.response.TokenResponse;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.error.ErrorCode;
import com.chaewookim.accountbookformoms.global.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("토큰 재발급 성공 테스트")
    void refreshToken_Success() {

        // given
        // 가짜 요청 객체
        String requestToken = "token";
        TokenReissueRequest request = new TokenReissueRequest(requestToken);

        // 기존 객체 정보
        Long userId = 1L;
        String email = "email@gmail.com";
        String newAccessToken = "newAccessToken";
        String newRefreshToken = "newRefreshToken";

        // User 객체
        User user = User.forTestBuilder()
                .id(userId)
                .username("username")
                .email(email)
                .password("passowrd")
                .isAdmin(false)
                .address("집")
                .build();

        // 명시적으로 userId 넣어줌
        ReflectionTestUtils.setField(user, "id", userId);

        // 저장된 객체 생성
        RefreshToken savedToken = RefreshToken.forTestBuilder()
                .userId(userId)
                .token(requestToken)
                .build();

        // 토큰 유효성 검사 통과
        given(jwtTokenProvider.validateToken(requestToken)).willReturn(true);

        // 토큰에서 이메일 추출
        given(jwtTokenProvider.getSubject(requestToken)).willReturn(email);

        // 이메일로 유저 찾기
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // 유저 id로 저장된 토큰 검색
        given(refreshTokenRepository.findByUserId(userId)).willReturn(Optional.of(savedToken));

        // 새 토큰 생성 결과 설정
        given(jwtTokenProvider.createAccessToken(any(Authentication.class))).willReturn(newAccessToken);
        given(jwtTokenProvider.createRefreshToken(any(Authentication.class))).willReturn(newRefreshToken);

        // when
        TokenResponse response = authService.reissue(request);

        // then
        // 응답에 새 토큰 잘 들어갔는지
        assertEquals(newAccessToken, response.accessToken());
        assertEquals(newRefreshToken, response.refreshToken());

        // dirty checking으로 기존 토큰 객체 값이 새 토큰으로 바뀌었는지 확인
        assertEquals(newRefreshToken, savedToken.getToken());
    }

    @Test
    @DisplayName("토큰 재발급 실패 테스트 - 유효하지 않은 토큰")
    void refreshToken_Failure_invalidToken() {

        // given
        String invalidToken = "invalidToken";
        TokenReissueRequest request = new TokenReissueRequest(invalidToken);

        // validateToken이 false를 return
        given(jwtTokenProvider.validateToken(invalidToken)).willReturn(false);

        // when & then
        // 예외 발생 검증
        CustomException exception = assertThrows(CustomException.class, () -> authService.reissue(request));

        // 예외 상세 검증
        assertEquals(ErrorCode.INVALID_REFRESH_TOKEN, exception.getErrorCode());
    }


    @Test
    @DisplayName("로그아웃 성공 테스트")
    void logout_Success() {

        // given
        // 요청 객체
        String requestToken = "token";
        LogoutRequest request = new LogoutRequest(requestToken);

        // 사용자 객체
        User user = User.forTestBuilder()
                .id(1L)
                .username("username")
                .email("email")
                .password("password")
                .isAdmin(false)
                .address("address")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        // 토큰 true 반환
        given(jwtTokenProvider.validateToken(requestToken)).willReturn(true);

        // when
        authService.logout(request);

        // then
        verify(refreshTokenRepository, times(1)).deleteByToken(requestToken);
    }

    @Test
    @DisplayName("로그아웃 실패 테스트 - 유효하지 않은 토큰")
    void logout_Failure_invalidToken() {

        // given
        String invalidToken = "invalidToken";

        // validate 반환값을 false로 설정
        given(jwtTokenProvider.validateToken(invalidToken)).willReturn(false);

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> authService.logout(new LogoutRequest(invalidToken)));
        assertEquals(ErrorCode.INVALID_REFRESH_TOKEN, exception.getErrorCode());
    }
}