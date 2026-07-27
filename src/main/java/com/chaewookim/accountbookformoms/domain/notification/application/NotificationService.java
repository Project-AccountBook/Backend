package com.chaewookim.accountbookformoms.domain.notification.application;

import com.chaewookim.accountbookformoms.domain.notification.dao.NotificationRepository;
import com.chaewookim.accountbookformoms.domain.notification.dao.UserDeviceRepository;
import com.chaewookim.accountbookformoms.domain.notification.dto.response.NotificationResponse;
import com.chaewookim.accountbookformoms.domain.notification.entity.Notification;
import com.chaewookim.accountbookformoms.domain.notification.enums.NotificationType;
import com.chaewookim.accountbookformoms.domain.notification.error.NotificationErrorCode;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final FcmService fcmService;

    @Transactional
    public void createNotification(User user, NotificationType type, String title, String message, String redirectUrl, Long referenceId) {

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .redirectUrl(redirectUrl)
                .referenceId(referenceId)
                .build();

        notificationRepository.save(notification);

        // FCM 발송
        userDeviceRepository.findByUser(user).ifPresent(device -> {
            Map<String, String> data = Map.of(
                    "redirectUrl", redirectUrl != null ? redirectUrl : "",
                    "referenceId", String.valueOf(referenceId)
            );
            fcmService.sendNotification(device.getFcmToken(), title, message, data);
        });
    }

    public Page<NotificationResponse> getNotifications(User user, Pageable pageable) {
        return notificationRepository.findByUser(user, pageable)
                .map(NotificationResponse::from);
    }

    public Long getUnreadCount(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long userId) {

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUser().getId().equals(userId)) {
            throw new CustomException(NotificationErrorCode.NOTIFICATION_ACCESS_DENIED);
        }

        notification.markAsRead();
        return NotificationResponse.from(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUser().getId().equals(userId)) {
            throw new CustomException(NotificationErrorCode.NOTIFICATION_ACCESS_DENIED);
        }

        notificationRepository.delete(notification);
    }

    @Transactional
    public void deleteAllByUserId(Long userId) {
        notificationRepository.deleteAll(notificationRepository.findAllByUserId(userId));
    }
}
