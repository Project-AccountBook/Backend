package com.chaewookim.accountbookformoms.domain.notification.event;

import com.chaewookim.accountbookformoms.domain.notification.application.NotificationService;
import com.chaewookim.accountbookformoms.domain.notification.enums.NotificationType;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.domain.user.error.UserErrorCode;
import com.chaewookim.accountbookformoms.domain.user.event.UserSignedUpEvent;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    @Async
    @EventListener
    public void handleNotificationEvent(NotificationEvent event) {

        notificationService.createNotification(
                event.user(),
                event.type(),
                event.title(),
                event.message(),
                event.redirectUrl(),
                event.referenceId()
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserSignedUpEvent(UserSignedUpEvent event) {

        User user = userRepository.findById(event.userId())
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        eventPublisher.publishEvent(new NotificationEvent(
                user,
                NotificationType.SYSTEM,
                "회원가입을 축하합니다!",
                "가계부 서비스에 오신 것을 환영해요.",
                null,
                null
        ));
    }
}
