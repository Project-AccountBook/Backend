package com.chaewookim.accountbookformoms.domain.notification.event;

import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionCategoryRepository;
import com.chaewookim.accountbookformoms.domain.asset.entity.TransactionCategory;
import com.chaewookim.accountbookformoms.domain.budget.application.BudgetService;
import com.chaewookim.accountbookformoms.domain.budget.event.BudgetExceededCheckEvent;
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

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class BudgetEventListener {

    private final BudgetService budgetService;
    private final UserRepository userRepository;
    private final TransactionCategoryRepository categoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBudgetCheckEvent(BudgetExceededCheckEvent event) {
        try {
            userRepository.findByIdWithNotificationAndSettings(event.userId()).ifPresent(user -> {
                if (user.getUserNotificationSetting() == null
                        || !Boolean.TRUE.equals(user.getUserNotificationSetting().getIsBudgetAlertEnabled())) {
                    return;
                }

                if (event.yearMonth().equals(user.getLastBudgetAlertMonth())) {
                    return;
                }

                if (user.getUserSetting() == null) {
                    return;
                }

                BigDecimal progress = budgetService.getCategoryProgress(
                        event.userId(), event.yearMonth(), event.categoryId());
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
                            "budget",
                            null
                    ));

                    user.updateLastBudgetAlertMonth(event.yearMonth());
                    userRepository.save(user);
                }
            });
        } catch (RuntimeException e) {
            log.error("예산 알림 처리 실패 userId={}, categoryId={}", event.userId(), event.categoryId(), e);
        }
    }
}
