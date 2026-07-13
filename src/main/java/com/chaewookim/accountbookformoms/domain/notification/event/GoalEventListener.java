package com.chaewookim.accountbookformoms.domain.notification.event;

import com.chaewookim.accountbookformoms.domain.asset.dao.AccountRepository;
import com.chaewookim.accountbookformoms.domain.asset.entity.Account;
import com.chaewookim.accountbookformoms.domain.asset.event.GoalAchievedCheckEvent;
import com.chaewookim.accountbookformoms.domain.notification.application.NotificationService;
import com.chaewookim.accountbookformoms.domain.notification.enums.NotificationType;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoalEventListener {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final NotificationService notificationService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGoalAchievedCheckEvent(GoalAchievedCheckEvent event) {
        try {
            process(event);
        } catch (Exception ex) {
            log.error("목표 달성 알림 처리 실패 userId={}, accountId={}", event.userId(), event.accountId(), ex);
        }
    }

    private void process(GoalAchievedCheckEvent event) {
        Account account = accountRepository.findByIdAndUserId(event.accountId(), event.userId())
                .filter(a -> a.getGoalAmount() != null && a.isGoalAchieved())
                .orElse(null);
        if (account == null) {
            return;
        }

        User user = userRepository.findByIdWithNotificationSetting(event.userId())
                .filter(User::isGoalAlertEnabled)
                .orElse(null);
        if (user == null) {
            return;
        }

        if (accountRepository.claimGoalAchievedNotification(event.accountId(), event.userId()) != 1) {
            return;
        }

        String message = String.format("[%s] 계좌 목표 금액에 도달했습니다!", account.getAccountName());

        notificationService.createNotification(
                user,
                NotificationType.GOAL,
                "목표 달성 알림",
                message,
                "goals",
                account.getId()
        );
    }
}
