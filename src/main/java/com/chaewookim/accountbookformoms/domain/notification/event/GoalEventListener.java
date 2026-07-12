package com.chaewookim.accountbookformoms.domain.notification.event;

import com.chaewookim.accountbookformoms.domain.asset.dao.AccountRepository;
import com.chaewookim.accountbookformoms.domain.asset.entity.Account;
import com.chaewookim.accountbookformoms.domain.asset.event.GoalAchievedCheckEvent;
import com.chaewookim.accountbookformoms.domain.notification.enums.NotificationType;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.UserNotificationSetting;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GoalEventListener {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Async
    @EventListener
    @Transactional
    public void handleGoalAchievedCheckEvent(GoalAchievedCheckEvent event) {

        Account account = accountRepository.findByIdAndUserId(event.accountId(), event.userId())
                .orElse(null);
        if (account == null || account.getGoalAmount() == null) {
            return;
        }

        boolean achieved = account.getCurrentBalance().compareTo(account.getGoalAmount()) >= 0;

        if (!achieved) {
            if (account.isGoalAchievedNotified()) {
                account.resetGoalAchievedNotified();
                accountRepository.save(account);
            }
            return;
        }

        if (account.isGoalAchievedNotified()) {
            return;
        }

        userRepository.findById(event.userId()).ifPresent(user -> {
            UserNotificationSetting setting = user.getUserNotificationSetting();
            if (setting == null || !Boolean.TRUE.equals(setting.getIsGoalAlertEnabled())) {
                return;
            }

            String message = String.format("[%s] 계좌 목표 금액에 도달했습니다!", account.getAccountName());

            eventPublisher.publishEvent(new NotificationEvent(
                    user,
                    NotificationType.GOAL,
                    "목표 달성 알림",
                    message,
                    "dashboard",
                    account.getId()
            ));

            account.markGoalAchievedNotified();
            accountRepository.save(account);
        });
    }
}
