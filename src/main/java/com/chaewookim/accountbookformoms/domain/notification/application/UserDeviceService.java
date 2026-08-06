package com.chaewookim.accountbookformoms.domain.notification.application;

import com.chaewookim.accountbookformoms.domain.notification.dao.UserDeviceRepository;
import com.chaewookim.accountbookformoms.domain.notification.entity.UserDevice;
import com.chaewookim.accountbookformoms.domain.notification.error.NotificationErrorCode;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDeviceService {

    private static final int FCM_TOKEN_MIN_LENGTH = 100;
    private static final int FCM_TOKEN_MAX_LENGTH = 250;
    private static final Pattern FCM_TOKEN_PATTERN = Pattern.compile("^[A-Za-z0-9_:\\-]+$");

    private final UserDeviceRepository userDeviceRepository;

    @Transactional
    public void registerToken(User user, String fcmToken) {
        validateFcmToken(fcmToken);
        userDeviceRepository.findByUserId(user.getId())
                .ifPresentOrElse(
                        device -> device.updateToken(fcmToken),
                        () -> userDeviceRepository.save(new UserDevice(user, fcmToken))
                );
    }

    @Transactional
    public void removeToken(User user) {
        userDeviceRepository.findByUserId(user.getId()).ifPresent(userDeviceRepository::delete);
    }

    private void validateFcmToken(String token) {
        if (token == null
                || token.length() < FCM_TOKEN_MIN_LENGTH
                || token.length() > FCM_TOKEN_MAX_LENGTH
                || !FCM_TOKEN_PATTERN.matcher(token).matches()) {
            throw new CustomException(NotificationErrorCode.INVALID_FCM_TOKEN);
        }
    }
}
