package com.chaewookim.accountbookformoms.domain.user.application;

import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.dto.request.SignupRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.request.UpdatePasswordRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.request.UpdateProfileRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.response.SignupResponse;
import com.chaewookim.accountbookformoms.domain.user.dto.response.UserProfileResponse;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.domain.user.entity.UserNotificationSetting;
import com.chaewookim.accountbookformoms.domain.user.entity.UserSetting;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserCommonService userCommonService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("회원가입 - 성공")
    void signUp_success_new_user() {

        // given
        SignupRequest request = new SignupRequest("test@email.com", "pw", "user", LocalDate.now(), "address");
        User savedUser = User.builder().email("test@email.com").username("user").build();

        given(userRepository.findByEmailIncludingDeleted(request.email())).willReturn(Optional.empty());
        given(passwordEncoder.encode(request.password())).willReturn("encoded");
        given(userCommonService.saveUser(any(), any(), any(), any(), any(), any())).willReturn(savedUser);

        // when
        SignupResponse response = userService.signUp(request);

        // then
        assertThat(response.email()).isEqualTo(request.email());
        verify(userCommonService).saveUser(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("삭제된 유저 복구 - 성공")
    void signUp_success_restore_user() {

        // given
        SignupRequest request = new SignupRequest("test@email.com", "pw", "restoredUser", LocalDate.now(), "address");
        User deletedUser = User.builder().email("test@email.com").build();
        deletedUser.delete();

        given(userRepository.findByEmailIncludingDeleted(request.email())).willReturn(Optional.of(deletedUser));
        given(userCommonService.restoreUser(any(), any(), any(), any(), any())).willReturn(deletedUser);

        // when
        SignupResponse response = userService.signUp(request);

        // then
        assertThat(response.email()).isEqualTo(request.email());
        verify(userCommonService).restoreUser(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("회원가입 - 이메일 중복 예외 발생")
    void signUp_fail_duplicate_email() {

        // given
        SignupRequest request = new SignupRequest("test@email.com", "pw", "user", LocalDate.now(), "address");
        User activeUser = User.builder().email("test@email.com").build();

        given(userRepository.findByEmailIncludingDeleted(request.email())).willReturn(Optional.of(activeUser));

        // when & then
        assertThatThrownBy(() -> userService.signUp(request)).isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("프로필 조회 -  성공")
    void getMyProfile_success() {

        // given
        User user = mock(User.class);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(user.getUserSetting()).willReturn(mock(UserSetting.class));
        given(user.getUserNotificationSetting()).willReturn(mock(UserNotificationSetting.class));

        // when
        UserProfileResponse response = userService.getMyProfile(1L);

        // then
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("프로필 수정 - 성공")
    void updateMyProfile_success() {

        // given
        User user = mock(User.class);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(user.getUserSetting()).willReturn(mock(UserSetting.class));
        given(user.getUserNotificationSetting()).willReturn(mock(UserNotificationSetting.class));
        UpdateProfileRequest request = new UpdateProfileRequest("new", LocalDate.now(), "newAddress", 50, true, true, true, true);

        // when
        userService.updateMyProfile(1L, request);

        // then
        verify(user).updateProfile(any(), any(), any());
    }

    @Test
    @DisplayName("비밀번호 변경 - 성공")
    void updatePassword_success() {

        // given
        User user = User.builder().password("encoded").build();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("old", "encoded")).willReturn(true);
        given(passwordEncoder.encode("new")).willReturn("newEncoded");

        // when
        userService.updatePassword(1L, new UpdatePasswordRequest("old", "new"));

        // then
        assertThat(user.getPassword()).isEqualTo("newEncoded");
    }

    @Test
    @DisplayName("회원 탈퇴 - 성공")
    void withdraw_success() {

        // given
        User user = mock(User.class);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when
        userService.withdraw(1L);

        // then
        verify(userRepository).delete(user);
    }
}