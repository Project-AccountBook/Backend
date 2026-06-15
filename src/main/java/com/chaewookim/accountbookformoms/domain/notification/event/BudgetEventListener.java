package com.chaewookim.accountbookformoms.domain.notification.event;

import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionCategoryRepository;
import com.chaewookim.accountbookformoms.domain.asset.entity.TransactionCategory;
import com.chaewookim.accountbookformoms.domain.budget.application.BudgetService;
import com.chaewookim.accountbookformoms.domain.budget.event.BudgetExceededCheckEvent;
import com.chaewookim.accountbookformoms.domain.notification.enums.NotificationType;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;


@Component
@RequiredArgsConstructor
public class BudgetEventListener {

    private final BudgetService budgetService;
    private final UserRepository userRepository;
    private final TransactionCategoryRepository categoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Async
    @EventListener
    public void handleBudgetCheckEvent(BudgetExceededCheckEvent event) {

        userRepository.findById(event.userId()).ifPresent(user -> {
            if (user.getUserNotificationSetting() != null && user.getUserNotificationSetting().getIsBudgetAlertEnabled()) {

                if (event.yearMonth().equals(user.getLastBudgetAlertMonth())) {
                    return;
                }

                BigDecimal progress = budgetService.getCategoryProgress(event.userId(), event.yearMonth(), event.categoryId());
                int alertThreshold = user.getUserSetting().getBudgetAlertThreshold();

                if (progress.compareTo(new BigDecimal(alertThreshold)) >= 0) {
                    String categoryName = categoryRepository.findById(event.categoryId())
                            .map(TransactionCategory::getName).orElse("해당 카테고리");

                    String message = String.format("이번 달 [%s] 예산 사용률이 %s%%에 도달했습니다.",
                            categoryName, progress.toBigInteger());

                    eventPublisher.publishEvent(new NotificationEvent(
                            user,
                            NotificationType.BUDGET,
                            "예산 알림",
                            message,
                            "/budget-page",
                            null
                    ));

                    user.updateLastBudgetAlertMonth(event.yearMonth());
                    userRepository.save(user);
                }
            }
        });
    }
}
