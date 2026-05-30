package com.chaewookim.accountbookformoms.domain.user.application;

import com.chaewookim.accountbookformoms.domain.user.dao.RefreshTokenRepository;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.dto.response.TokenResponse;
import com.chaewookim.accountbookformoms.domain.user.entity.RefreshToken;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.domain.user.dto.request.LoginRequest;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.error.ErrorCode;
import com.chaewookim.accountbookformoms.global.security.jwt.JwtTokenProvider;
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

//    @Transactional
//    public TokenResponse reissue(@Valid TokenReissueRequest request) {
//
//        // refresh token 자체를 검증
//        if (!jwtTokenProvider.validateToken(request.refreshToken())) {
//            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
//        }
//
//        // 이메일 추출
//        String email = jwtTokenProvider.getSubject(request.refreshToken());
//
//        // DB에 저장된 refresh token 가져오기
//        RefreshToken savedToken = refreshTokenRepository.findByUserId(getUserIdByEmail(email))
//                .orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));
//
//        // 요청 토큰과 db 토큰이 같은지 확인
//        if (!savedToken.getToken().equals(request.refreshToken())) {
//            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
//        }
//
//        // 추출한 이메일로 User 객체 생성
//        User user = userRepository.findByEmail(email).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
//
//        // authentication 생성
//        Authentication authentication = new UsernamePasswordAuthenticationToken(user
//                .getEmail(),
//                null,
//                Collections.singleton(new SimpleGrantedAuthority(user.getRole().getKey()))
//        );
//
//        // 보안을 위해 토큰 둘 다 새로 발급
//        String newAccessToken = jwtTokenProvider.createAccessToken(authentication);
//        String newRefreshToken = jwtTokenProvider.createRefreshToken(authentication);
//
//        // 기존 것 지우고 저장
//        savedToken.updateToken(newRefreshToken);
//
//        return new TokenResponse(newAccessToken, newRefreshToken);
//    }
//
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
