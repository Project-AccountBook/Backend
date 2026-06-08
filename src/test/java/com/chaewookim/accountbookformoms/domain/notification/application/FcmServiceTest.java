package com.chaewookim.accountbookformoms.domain.notification.application;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FcmServiceTest {

    @InjectMocks
    private FcmService fcmService;

    @Test
    @DisplayName("FCM 발송 - 성공")
    void sendNotificationTest() throws Exception {

        // given
        String token = "test-token";
        String title = "제목";
        String body = "본문";
        Map<String, String> data = Map.of("key", "value");

        try (MockedStatic<FirebaseMessaging> firebaseMessaging = mockStatic(FirebaseMessaging.class)) {
            FirebaseMessaging mockMessaging = mock(FirebaseMessaging.class);
            firebaseMessaging.when(FirebaseMessaging::getInstance).thenReturn(mockMessaging);

            // when
            fcmService.sendNotification(token, title, body, data);

            // then
            verify(mockMessaging, times(1)).send(any(Message.class));
        }
    }
}