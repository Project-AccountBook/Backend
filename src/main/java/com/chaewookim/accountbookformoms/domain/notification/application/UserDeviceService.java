package com.chaewookim.accountbookformoms.domain.notification.application;

import com.chaewookim.accountbookformoms.domain.notification.dao.UserDeviceRepository;
import com.chaewookim.accountbookformoms.domain.notification.entity.UserDevice;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDeviceService {

    private final UserDeviceRepository userDeviceRepository;

    @Transactional
    public void registerToken(User user, String fcmToken) {
        userDeviceRepository.findByUser(user)
                .ifPresentOrElse(
                        device -> device.updateToken(fcmToken),
                        () -> userDeviceRepository.save(new UserDevice(user, fcmToken))
                );
    }

    @Transactional
    public void removeToken(User user) {
        userDeviceRepository.findByUser(user).ifPresent(userDeviceRepository::delete);
    }
}
