package com.chaewookim.accountbookformoms.domain.notification.event;

import com.chaewookim.accountbookformoms.domain.grouppruchase.event.GroupPurchaseCreatedEvent;
import com.chaewookim.accountbookformoms.domain.notification.enums.NotificationType;
import com.chaewookim.accountbookformoms.domain.user.dao.InterestCategoryRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class GroupPurchaseEventListener {

    private final InterestCategoryRepository interestCategoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGroupPurchaseCreatedEvent(GroupPurchaseCreatedEvent event) {

        var subscribers = interestCategoryRepository
                .findByCoopCategoryNameAndIsAlarmEnabledTrue(event.categoryName());

        subscribers.forEach(sub -> {
            User user = sub.getUser();

            if (Boolean.TRUE.equals(user.getUserNotificationSetting().getIsInterestCategoryEnabled())) {
                eventPublisher.publishEvent(new NotificationEvent(
                        user,
                        NotificationType.INTEREST_CATEGORY,
                        "새 공동구매 알림",
                        event.title(),
                        "/group-purchase/" + event.groupPurchaseId(),
                        event.groupPurchaseId()
                ));
            }
        });
    }
}
