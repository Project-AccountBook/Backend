package com.chaewookim.accountbookformoms.domain.notification.event;

import com.chaewookim.accountbookformoms.domain.asset.enums.FixedTransactionExecutionFailure;
import com.chaewookim.accountbookformoms.domain.asset.event.FixedTransactionExecutionFailedEvent;
import com.chaewookim.accountbookformoms.domain.notification.enums.NotificationType;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class FixedTransactionEventListener {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleExecutionFailed(FixedTransactionExecutionFailedEvent event) {
        try {
            userRepository.findByIdWithNotificationAndSettings(event.userId()).ifPresent(user -> {
                if (user.getUserNotificationSetting() == null
                        || !Boolean.TRUE.equals(user.getUserNotificationSetting().getIsSystemAlertEnabled())) {
                    return;
                }

                String reasonText = event.failureReason() == FixedTransactionExecutionFailure.CREDIT_LIMIT_EXCEEDED
                        ? "신용카드 한도가 부족합니다"
                        : "잔액이 부족합니다";
                String description = (event.description() == null || event.description().isBlank())
                        ? "고정 거래"
                        : event.description();
                String message = String.format("[%s] %s 실행에 실패했습니다. %s.",
                        event.accountName(), description, reasonText);

                eventPublisher.publishEvent(new NotificationEvent(
                        user,
                        NotificationType.SYSTEM,
                        "고정 거래 실행 실패",
                        message,
                        "history/fixed",
                        event.fixedTransactionId()
                ));
            });
        } catch (RuntimeException e) {
            log.error("고정 거래 실패 알림 처리 실패 fixedTransactionId={}", event.fixedTransactionId(), e);
        }
    }
}
