package com.chaewookim.accountbookformoms.domain.user.application;

import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.domain.user.enums.SocialProvider;
import com.chaewookim.accountbookformoms.domain.user.event.UserSignedUpEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserCommonServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserCommonService userCommonService;

    @Test
    @DisplayName("회원 저장 - 성공")
    void saveUser_success() {

        // given
        User user = User.builder().email("test@email.com").build();
        given(userRepository.save(any(User.class))).willReturn(user);

        // when
        User result = userCommonService.saveUser("test@email.com", "pw", "user", SocialProvider.LOCAL, LocalDate.now(), "address");

        // then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
        verify(eventPublisher).publishEvent(any(UserSignedUpEvent.class));
    }

    @Test
    @DisplayName("소셜 회원 가입 - 성공")
    void saveSocialUser_success() {

        // given
        User user = User.builder().email("social@email.com").provider(SocialProvider.GOOGLE).build();
        given(userRepository.save(any(User.class))).willReturn(user);

        // when
        User result = userCommonService.saveSocialUser("social@email.com", "socialUser", SocialProvider.GOOGLE);

        // then
        assertThat(result.getEmail()).isEqualTo("social@email.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("회원 복구 - 성공")
    void restoreUser_success() {

        // given
        User user = User.builder().email("test@email.com").build();
        user.delete();
        given(userRepository.save(any(User.class))).willReturn(user);

        // when
        User result = userCommonService.restoreUser(user, "newNickname", null, null, null);

        // then
        assertThat(result.getDeletedAt()).isNull();
        assertThat(result.getUsername()).isEqualTo("newNickname");
        assertThat(result.getPassword()).isNull();
        verify(userRepository).save(any(User.class));
        verify(eventPublisher).publishEvent(any(UserSignedUpEvent.class));
    }
}