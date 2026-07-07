package com.chaewookim.accountbookformoms.domain.notification.event;

import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.GroupPurchaseCategoryRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.Category;
import com.chaewookim.accountbookformoms.domain.grouppruchase.event.GroupPurchaseCreatedEvent;
import com.chaewookim.accountbookformoms.domain.notification.enums.NotificationType;
import com.chaewookim.accountbookformoms.domain.user.dao.InterestCategoryRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.domain.user.entity.UserNotificationSetting;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.error.ErrorCode;
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
    private final GroupPurchaseCategoryRepository groupPurchaseCategoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGroupPurchaseCreatedEvent(GroupPurchaseCreatedEvent event) {

        Category category = groupPurchaseCategoryRepository.findById(event.categoryId())
                .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));

        var subscribers = interestCategoryRepository.findByCategoryAndIsAlarmEnabledTrue(category);

        subscribers.forEach(sub -> {
            User user = sub.getUser();
            UserNotificationSetting setting = user.getUserNotificationSetting();

            if (setting != null && Boolean.TRUE.equals(setting.getIsInterestCategoryEnabled()) && sub.isAlarmEnabled()) {
                eventPublisher.publishEvent(new NotificationEvent(
                        user,
                        NotificationType.INTEREST_CATEGORY,
                        "새 공동구매 알림",
                        event.title(),
                        "groupbuy",
                        event.groupPurchaseId()
                ));
            }
        });
    }
}
