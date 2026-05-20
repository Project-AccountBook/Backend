package com.chaewookim.accountbookformoms.domain.user.application;

import com.chaewookim.accountbookformoms.domain.user.dao.RefreshTokenRepository;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.domain.RefreshToken;
import com.chaewookim.accountbookformoms.domain.user.domain.User;
import com.chaewookim.accountbookformoms.domain.user.dto.request.TokenReissueRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.request.UpdateRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.request.WithdrawRequest;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    // 이게 사용할 것. 사용할 것에 가짜 객체들 주입
    @InjectMocks
    private UserService userService;

    // 가짜 레포지토리
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("회원 정보 수정(username, email, address) 성공 테스트")
    void updateUser_Success() {

        // given: 준비
        // 가짜 객체에 넣을 값들을 준비
        String email = "updateEmail@gmail.com";
        String username = "updateUsername";
        String address = "updateAddress";
        Long userId = 1L;

        // 수정할 데이터 요청 객체 생성
        UpdateRequest updateRequest = new UpdateRequest(username, email, address);

        // 기존 유저 객체 생성
        String originEmail = "originEmail";
        User existingUser = User.forTestBuilder()
                .id(userId)
                .username("username")
                .email(originEmail)
                .password("password")
                .address("address")
                .build();

        // 테스트 환경에서 id에 값을 넣어주지 못할 경우가 존재할 수 있기 때문에 강제 주입
        ReflectionTestUtils.setField(existingUser, "id", userId);

        // userRepository.findByEmail() 호출 시 existingUser를 리턴하라고 조작
        given(userRepository.findByEmail(originEmail)).willReturn(Optional.of(existingUser));

        // when: 실행
        Long updateUserId = userService.updateUser(originEmail, updateRequest);

        // then: 검증
        // 리턴된 ID가 올바른지 확인
        assertEquals(userId, updateUserId);

        // 실제 객체 내용이 바뀌었는지 확인
        assertEquals(email, existingUser.getEmail());
        assertEquals(address, existingUser.getAddress());
        assertEquals(username, existingUser.getUsername());
    }


    @Test
    @DisplayName("회원 탈퇴 성공 테스트")
    void withdrawUser_Success() {

        // given
        // 요청 객체
        String email = "originEmail";
        WithdrawRequest withdrawRequest = new WithdrawRequest("password", "reason");

        // UserDetails
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(email)
                .password("password")
                .roles("USER")
                .build();

        // 사용자 객체
        Long userId = 1L;
        User user = User.forTestBuilder()
                .id(userId)
                .username("username")
                .email(email)
                .password("password")
                .isAdmin(false)
                .address("address")
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        // 비밀번호 검증 true
        given(passwordEncoder.matches(withdrawRequest.password(), user.getPassword())).willReturn(true);

        // user 조회
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // when & then
        userService.withdrawUser(userDetails, withdrawRequest);

        verify(refreshTokenRepository, times(1)).deleteByUserId(userId);
        verify(userRepository, times(1)).delete(user);
    }

    @Test
    @DisplayName("회원 탈퇴 실패 테스트 - 비밀번호 불일치")
    void withdrawUser_Failure_passwordNotMatch() {

        // given
        // request obj
        String email = "originEmail";
        String password = "password";
        WithdrawRequest withdrawRequest = new WithdrawRequest(password, "reason");

        // UserDetails obj
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(email)
                .password(password)
                .roles("USER")
                .build();

        // User obj
        Long userId = 1L;
        User user = User.forTestBuilder()
                .id(userId)
                .username("username")
                .email(email)
                .password("password")
                .isAdmin(false)
                .address("address")
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        // return User obj
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // 비밀번호 검증 - 실패로 설정
        given(passwordEncoder.matches(withdrawRequest.password(), user.getPassword())).willReturn(false);

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> userService.withdrawUser(userDetails, withdrawRequest));
        assertEquals(ErrorCode.PASSWORD_NOT_MATCH, exception.getErrorCode());
    }
}