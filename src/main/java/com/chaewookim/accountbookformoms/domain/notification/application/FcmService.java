    package com.chaewookim.accountbookformoms.domain.notification.application;

    import com.google.firebase.messaging.FirebaseMessaging;
    import com.google.firebase.messaging.FirebaseMessagingException;
    import com.google.firebase.messaging.Message;
    import com.google.firebase.messaging.Notification;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

    import java.util.Map;

    @Slf4j
    @Service
    @RequiredArgsConstructor
    @Transactional(readOnly = true)
    public class FcmService {

        public void sendNotification(String token, String title, String body, Map<String, String> data) {

            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data)
                    .build();

            try {
                FirebaseMessaging.getInstance().send(message);
                log.info("FCM 발송 시도 - 토큰: {}, 제목: {}, 내용: {}", token, title, body);
            } catch (FirebaseMessagingException e) {
                log.error("FCM 전송 실패: {}", e.getMessage());
            }
        }
    }
