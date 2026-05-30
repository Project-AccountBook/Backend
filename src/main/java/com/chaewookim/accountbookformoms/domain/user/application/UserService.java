package com.chaewookim.accountbookformoms.domain.user.application;

import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.dto.request.SignupRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.response.SignupResponse;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.error.ErrorCode;
import com.chaewookim.accountbookformoms.global.event.UserSignedUpEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
//    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public SignupResponse signUp(SignupRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        String encoded = passwordEncoder.encode(request.password());

        User user = User.builder()
                .email(request.email())
                .password(encoded)
                .username(request.username())
                .birthDate(request.birthDate())
                .address(request.address())
                .build();

        User savedUser = userRepository.save(user);
        eventPublisher.publishEvent(new UserSignedUpEvent(savedUser.getId()));
        return new SignupResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getUsername());
    }

//    public User getUserByUsername(String username) {
//
//        return userRepository.findByUsername(username).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
//    }
//
//    @Transactional
//    public Long updateUser(String username, @Valid UpdateRequest request) {
//
//        return userRepository.findByUsername(username).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND))
//                .updateUser(request)
//                .getId();
//    }
//
//    @Transactional
//    public void withdrawUser(UserDetails userDetails, @Valid WithdrawRequest request) {
//
//        String username = userDetails.getUsername();
//        User user = userRepository.findByUsername(username)
//                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
//
//        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
//            throw new CustomException(ErrorCode.PASSWORD_NOT_MATCH);
//        }
//
//        refreshTokenRepository.deleteByUserId(user.getId());
//        userRepository.delete(user);
//    }
}
