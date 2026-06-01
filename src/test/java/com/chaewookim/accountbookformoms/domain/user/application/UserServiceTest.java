package com.chaewookim.accountbookformoms.domain.user.application;

import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.dto.request.SignupRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.response.SignupResponse;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.event.UserSignedUpEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("회원가입_성공")
    void signUp_success() {

        // given
        SignupRequest request = new SignupRequest("test@email.com", "password123", "테스터", LocalDate.now(), "서울시");
        User savedUser = User.builder().email(request.email()).username(request.username()).build();

        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // when
        SignupResponse response = userService.signUp(request);

        // then
        assertThat(response.email()).isEqualTo(request.email());
        assertThat(response.username()).isEqualTo(request.username());
        verify(userRepository).save(any(User.class));
        verify(eventPublisher).publishEvent(any(UserSignedUpEvent.class));
    }

    @Test
    @DisplayName("회원가입_이메일 중복 예외 발생")
    void signUp_fail_duplicate_email() {

        // given
        SignupRequest request = new SignupRequest("test@email.com", "password123", "테스터", LocalDate.now(), "서울시");
        given(userRepository.existsByEmail(request.email())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.signUp(request)).isInstanceOf(CustomException.class);
    }
}