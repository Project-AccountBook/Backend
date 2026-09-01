package com.chaewookim.accountbookformoms.domain.user.application;

import com.chaewookim.accountbookformoms.domain.user.dao.RefreshTokenRepository;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.dto.request.LoginRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.request.ReissueRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.response.TokenResponse;
import com.chaewookim.accountbookformoms.domain.user.entity.RefreshToken;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.domain.user.enums.VerificationType;
import com.chaewookim.accountbookformoms.domain.user.error.UserErrorCode;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.security.jwt.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final String OAUTH_CODE_KEY_PREFIX = "OAUTH2:CODE:";

    private final EmailVerificationService emailVerificationService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public TokenResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        if (!encoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(UserErrorCode.USER_NOT_FOUND);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getRole().name());
        String refreshTokenValue = jwtTokenProvider.createRefreshToken(user.getEmail());

        RefreshToken refreshToken = new RefreshToken(user.getEmail(), refreshTokenValue);
        refreshTokenRepository.save(refreshToken);

        log.info("Redis에 리프레시 토큰 저장 성공 - 유저: {}", user.getEmail());
        return new TokenResponse(accessToken, refreshTokenValue);
    }

    @Transactional
    public TokenResponse exchangeOAuthCode(String code) {
        if (code == null || code.isBlank()) {
            throw new CustomException(UserErrorCode.INVALID_OAUTH_CODE);
        }

        String value = redisTemplate.opsForValue().getAndDelete(OAUTH_CODE_KEY_PREFIX + code);
        if (value == null || value.isBlank()) {
            throw new CustomException(UserErrorCode.INVALID_OAUTH_CODE);
        }

        String[] parts = value.split("\n", 3);
        if (parts.length != 3) {
            throw new CustomException(UserErrorCode.INVALID_OAUTH_CODE);
        }

        return new TokenResponse(parts[0], parts[1], Boolean.parseBoolean(parts[2]));
    }

    @Transactional
    public TokenResponse reissue(@Valid ReissueRequest request) {

        String refreshTokenValue = request.refreshToken();

        if (!jwtTokenProvider.validateToken(request.refreshToken())) {
            throw new CustomException(UserErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshToken savedToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new CustomException(UserErrorCode.INVALID_REFRESH_TOKEN));

        User user = userRepository.findByEmail(savedToken.getEmail())
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        String newAccessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getRole().name());
        String newRefreshTokenValue = jwtTokenProvider.createRefreshToken(user.getEmail());

        refreshTokenRepository.delete(savedToken);
        RefreshToken newRefreshToken = new RefreshToken(user.getEmail(), newRefreshTokenValue);
        refreshTokenRepository.save(newRefreshToken);

        return new TokenResponse(newAccessToken, newRefreshTokenValue);
    }

    @Transactional
    public void sendSignupVerificationCode(String email, String clientIp) {

        if (userRepository.findByEmail(email).isPresent()) {
            throw new CustomException(UserErrorCode.DUPLICATE_EMAIL);
        }

        emailVerificationService.sendVerificationCode(email, VerificationType.SIGNUP, clientIp);
    }

    @Transactional
    public void requestPasswordReset(String email, String clientIp) {
        userRepository.findByEmail(email).orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        emailVerificationService.sendVerificationCode(email, VerificationType.RESET, clientIp);
    }

    @Transactional
    public void resetPassword(String email, String code, String newPassword) {

        boolean verified = emailVerificationService.isVerified(email, VerificationType.RESET)
                || emailVerificationService.verifyCode(email, code, VerificationType.RESET);

        if (!verified) {
            throw new CustomException(UserErrorCode.INVALID_VERIFICATION_CODE);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        user.updatePassword(encoder.encode(newPassword));

        emailVerificationService.deleteVerification(email, VerificationType.RESET);
    }

    @Transactional
    public void logout(String email) {
        refreshTokenRepository.deleteById(email);
    }
}
