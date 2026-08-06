package com.chaewookim.accountbookformoms.domain.notification.application;

import com.chaewookim.accountbookformoms.domain.notification.dao.UserDeviceRepository;
import com.chaewookim.accountbookformoms.domain.notification.entity.UserDevice;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDeviceServiceTest {

    private static final String VALID_TOKEN = "a".repeat(150);

    @Mock
    private UserDeviceRepository userDeviceRepository;

    @InjectMocks
    private UserDeviceService userDeviceService;

    @Test
    @DisplayName("기존 기기 토큰이 없으면 새로 저장")
    void registerToken_save() {

        // given
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(userDeviceRepository.findByUserId(1L)).thenReturn(Optional.empty());

        // when
        userDeviceService.registerToken(user, VALID_TOKEN);

        // then
        verify(userDeviceRepository, times(1)).save(any(UserDevice.class));
    }

    @Test
    @DisplayName("기존 기기 토큰이 있으면 업데이트")
    void registerToken_update() {

        // given
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        UserDevice device = mock(UserDevice.class);

        when(userDeviceRepository.findByUserId(1L)).thenReturn(Optional.of(device));

        // when
        userDeviceService.registerToken(user, VALID_TOKEN);

        // then
        verify(device, times(1)).updateToken(VALID_TOKEN);
        verify(userDeviceRepository, never()).save(any(UserDevice.class));
    }

    @Test
    @DisplayName("FCM 토큰 형식 검증 실패 시 예외")
    void registerToken_invalidFormat() {
        User user = mock(User.class);

        assertThatThrownBy(() -> userDeviceService.registerToken(user, "short"))
                .isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> userDeviceService.registerToken(user, "a".repeat(150) + " space"))
                .isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> userDeviceService.registerToken(user, null))
                .isInstanceOf(CustomException.class);
        verify(userDeviceRepository, never()).save(any(UserDevice.class));
    }
}