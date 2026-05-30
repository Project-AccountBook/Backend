package com.chaewookim.accountbookformoms.domain.user.application;

import com.chaewookim.accountbookformoms.domain.user.dao.RefreshTokenRepository;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.dto.request.ReissueRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.response.TokenResponse;
import com.chaewookim.accountbookformoms.domain.user.entity.RefreshToken;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.domain.user.dto.request.LoginRequest;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.error.ErrorCode;
import com.chaewookim.accountbookformoms.global.security.jwt.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public TokenResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!encoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getRole().name());
        String refreshTokenValue = jwtTokenProvider.createRefreshToken(user.getEmail());

        RefreshToken refreshToken = new RefreshToken(user.getEmail(), refreshTokenValue);
        refreshTokenRepository.save(refreshToken);

        log.info("Redis에 리프레시 토큰 저장 성공 - 유저: {}", user.getEmail());
        return new TokenResponse(accessToken, refreshTokenValue);
    }

    @Transactional
    public TokenResponse reissue(@Valid ReissueRequest request) {

        String refreshTokenValue = request.refreshToken();

        if (!jwtTokenProvider.validateToken(request.refreshToken())) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshToken savedToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));

        User user = userRepository.findByEmail(savedToken.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String newAccessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getRole().name());
        String newRefreshTokenValue = jwtTokenProvider.createRefreshToken(user.getEmail());

        refreshTokenRepository.delete(savedToken);
        RefreshToken newRefreshToken = new RefreshToken(user.getEmail(), newRefreshTokenValue);
        refreshTokenRepository.save(newRefreshToken);

        return new TokenResponse(newAccessToken, newRefreshTokenValue);
    }

//    private Long getUserIdByEmail(String email) {
//
//        return userRepository.findByEmail(email).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND))
//                .getId();
//    }
//
//    @Transactional
//    public Void logout(LogoutRequest request) {
//
//        if (!jwtTokenProvider.validateToken(request.refreshToken())) {
//            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
//        }
//
//        refreshTokenRepository.deleteByToken(request.refreshToken());
//
//        return null;
//    }
}
