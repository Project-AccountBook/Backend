package com.chaewookim.accountbookformoms.domain.notification.application;

import com.chaewookim.accountbookformoms.domain.notification.dao.NotificationRepository;
import com.chaewookim.accountbookformoms.domain.notification.dao.UserDeviceRepository;
import com.chaewookim.accountbookformoms.domain.notification.dto.response.NotificationResponse;
import com.chaewookim.accountbookformoms.domain.notification.entity.Notification;
import com.chaewookim.accountbookformoms.domain.notification.entity.UserDevice;
import com.chaewookim.accountbookformoms.domain.notification.enums.NotificationType;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserDeviceRepository userDeviceRepository;

    @Mock
    private FcmService fcmService;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("알림 생성 - 성공")
    void createNotification_success() {

        // given
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        UserDevice device = UserDevice.builder().fcmToken("token").build();
        when(userDeviceRepository.findByUserId(1L)).thenReturn(Optional.of(device));

        // when
        notificationService.createNotification(user, NotificationType.SYSTEM, "제목", "본문", "/url", 1L);

        // then
        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(fcmService, times(1)).sendNotification(eq("token"), anyString(), anyString(), any(Map.class));
    }

    @Test
    @DisplayName("알림 생성 - FCM 실패해도 DB 저장은 유지")
    void createNotification_persistsWhenFcmFails() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        UserDevice device = UserDevice.builder().fcmToken("token").build();
        when(userDeviceRepository.findByUserId(1L)).thenReturn(Optional.of(device));
        doThrow(new IllegalStateException("Firebase not initialized")).when(fcmService)
                .sendNotification(eq("token"), anyString(), anyString(), any(Map.class));

        notificationService.createNotification(user, NotificationType.GOAL, "제목", "본문", "/url", 1L);

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("알림 목록 조회 - 성공")
    void getNotifications_success() {

        // given
        User user = mock(User.class);
        Pageable pageable = mock(Pageable.class);
        Notification notification = mock(Notification.class);
        when(notification.getType()).thenReturn(NotificationType.SYSTEM);
        when(notification.getId()).thenReturn(1L);
        when(notification.getTitle()).thenReturn("제목");
        when(notificationRepository.findByUser(user, pageable)).thenReturn(new PageImpl<>(List.of(notification)));

        // when
        Page<NotificationResponse> result = notificationService.getNotifications(user, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("읽지 않은 알림 개수 조회 - 성공")
    void getUnreadCount_success() {

        // given
        User user = mock(User.class);
        when(notificationRepository.countByUserAndIsReadFalse(user)).thenReturn(5L);

        // when
        Long count = notificationService.getUnreadCount(user);

        // when & then
        assertThat(count).isEqualTo(5L);
    }

    @Test
    @DisplayName("알림 읽음 처리 - 성공")
    void markAsRead_success() {

        // given
        Notification notification = mock(Notification.class);
        User user = mock(User.class);
        when(notification.getType()).thenReturn(NotificationType.SYSTEM);
        when(user.getId()).thenReturn(1L);
        when(notification.getUser()).thenReturn(user);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        // when
        notificationService.markAsRead(1L, 1L);

        // then
        verify(notification, times(1)).markAsRead();
    }

    @Test
    @DisplayName("알림 읽음 처리 - 타인의 알림을 읽음 처리 시 예외 발생")
    void markAsRead_accessDenied() {

        // given
        Notification notification = mock(Notification.class);
        User user = mock(User.class);
        when(user.getId()).thenReturn(2L);
        when(notification.getUser()).thenReturn(user);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        // when & then
        assertThrows(CustomException.class, () -> notificationService.markAsRead(1L, 1L));
    }

    @Test
    @DisplayName("모든 알림 읽음 처리 - 성공")
    void markAllAsRead_success() {

        // given
        Long userId = 1L;

        // when
        notificationService.markAllAsRead(userId);

        // then
        verify(notificationRepository, times(1)).markAllAsReadByUserId(userId);
    }

    @Test
    @DisplayName("알림 삭제 - 성공")
    void deleteNotification_success() {

        // given
        Notification notification = mock(Notification.class);
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(notification.getUser()).thenReturn(user);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        // when
        notificationService.deleteNotification(1L, 1L);

        // then
        verify(notificationRepository, times(1)).delete(notification);
    }
}