package com.chaewookim.accountbookformoms.domain.notification.application;

import com.chaewookim.accountbookformoms.domain.notification.dao.UserDeviceRepository;
import com.chaewookim.accountbookformoms.domain.notification.entity.UserDevice;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDeviceServiceTest {

    @Mock
    private UserDeviceRepository userDeviceRepository;

    @InjectMocks
    private UserDeviceService userDeviceService;

    @Test
    @DisplayName("기존 기기 토큰이 없으면 새로 저장")
    void registerToken_save() {

        // given
        User user = mock(User.class);
        String newToken = "new-token";
        when(userDeviceRepository.findByUser(user)).thenReturn(Optional.empty());

        // when
        userDeviceService.registerToken(user, newToken);

        // then
        verify(userDeviceRepository, times(1)).save(any(UserDevice.class));
    }

    @Test
    @DisplayName("기존 기기 토큰이 있으면 업데이트")
    void registerToken_update() {

        // given
        User user = mock(User.class);
        UserDevice device = mock(UserDevice.class);
        String newToken = "new-token";

        when(userDeviceRepository.findByUser(user)).thenReturn(Optional.of(device));

        // when
        userDeviceService.registerToken(user, newToken);

        // then
        verify(device, times(1)).updateToken(newToken);
        verify(userDeviceRepository, never()).save(any(UserDevice.class));
    }
}