package com.chaewookim.accountbookformoms.domain.user.application;

import com.chaewookim.accountbookformoms.domain.user.dao.RefreshTokenRepository;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.domain.User;
import com.chaewookim.accountbookformoms.domain.user.dto.request.SignUpRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.request.UpdateRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.request.WithdrawRequest;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.error.ErrorCode;
import com.chaewookim.accountbookformoms.global.event.UserSignedUpEvent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Long signUp(SignUpRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        String encoded = passwordEncoder.encode(request.password());

        User user = User.builder()
                .email(request.email())
                .username(request.username())
                .password(encoded)
                .birthDate(request.birthDate())
                .address(request.address())
                .build();

        User savedUser = userRepository.save(user);

        eventPublisher.publishEvent(new UserSignedUpEvent(savedUser.getId()));

        return userRepository.findByEmail(request.email()).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)).getId();
    }

    public User getUserByUsername(String username) {

        return userRepository.findByUsername(username).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public Long updateUser(String username, @Valid UpdateRequest request) {

        return userRepository.findByUsername(username).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND))
                .updateUser(request)
                .getId();
    }

    @Transactional
    public void withdrawUser(UserDetails userDetails, @Valid WithdrawRequest request) {

        // 현재 로그인된 유저 찾기
        String username = userDetails.getUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.PASSWORD_NOT_MATCH);
        }

        // 리프레시 토큰 정리
        refreshTokenRepository.deleteByUserId(user.getId());

        // 삭제 진행
        userRepository.delete(user);
    }
}
