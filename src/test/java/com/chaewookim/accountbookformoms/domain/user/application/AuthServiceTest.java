package com.chaewookim.accountbookformoms.domain.user.application;

import com.chaewookim.accountbookformoms.domain.user.dao.RefreshTokenRepository;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.dto.request.LoginRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.request.ReissueRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.response.TokenResponse;
import com.chaewookim.accountbookformoms.domain.user.entity.RefreshToken;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.domain.user.enums.UserRole;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("로그인_성공")
    void login_success() {

        // given
        LoginRequest request = new LoginRequest("test@email.com", "password123");
        User user = User.builder().email("test@email.com").password("encoded").role(UserRole.ROLE_USER).build();

        given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
        given(encoder.matches(request.password(), user.getPassword())).willReturn(true);
        given(jwtTokenProvider.createAccessToken(any(), any())).willReturn("new-access");
        given(jwtTokenProvider.createRefreshToken(any())).willReturn("new-refresh");

        // when
        TokenResponse response = authService.login(request);

        // then
        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("토큰 재발급_성공")
    void reissue_success() {

        // given
        String oldToken = "old-refresh";
        String email = "test@email.com";
        ReissueRequest request = new ReissueRequest(oldToken);
        RefreshToken savedToken = new RefreshToken(email, oldToken);
        User user = User.builder().email(email).role(UserRole.ROLE_USER).build();

        given(jwtTokenProvider.validateToken(oldToken)).willReturn(true);
        given(refreshTokenRepository.findByToken(oldToken)).willReturn(Optional.of(savedToken));
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(jwtTokenProvider.createAccessToken(any(), any())).willReturn("new-access");
        given(jwtTokenProvider.createRefreshToken(any())).willReturn("new-refresh");

        // when
        TokenResponse response = authService.reissue(request);

        // then
        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        verify(refreshTokenRepository).delete(savedToken);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("토큰 재발급_유효하지 않은 토큰 예외 발생")
    void reissue_fail_invalid_token() {

        // given
        ReissueRequest request = new ReissueRequest("invalid-token");
        given(jwtTokenProvider.validateToken(any())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.reissue(request)).isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("로그아웃_성공")
    void logout_success() {

        // given
        String email = "test@email.com";

        // when
        authService.logout(email);

        // then
        verify(refreshTokenRepository).deleteById(email);
    }
}