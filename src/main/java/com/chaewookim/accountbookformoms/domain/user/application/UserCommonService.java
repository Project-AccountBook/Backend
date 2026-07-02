package com.chaewookim.accountbookformoms.domain.user.application;

import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.domain.user.entity.UserNotificationSetting;
import com.chaewookim.accountbookformoms.domain.user.entity.UserSetting;
import com.chaewookim.accountbookformoms.domain.user.enums.SocialProvider;
import com.chaewookim.accountbookformoms.domain.user.event.UserSignedUpEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class UserCommonService {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    // 회원 저장
    private User createBaseUser(String email, String password, String username, SocialProvider provider, LocalDate birthDate, String address) {
        User user = User.builder()
                .email(email)
                .password(password)
                .username(username)
                .provider(provider)
                .birthDate(birthDate)
                .address(address)
                .build();

        user.setSettings(UserSetting.builder().user(user).build(),
                UserNotificationSetting.builder().user(user).build());

        return userRepository.save(user);
    }

    // 일반 회원가입
    public User saveLocalUser(String email, String password, String username, SocialProvider provider, LocalDate birthDate, String address) {
        User savedUser = createBaseUser(email, password, username, provider, birthDate, address);
        eventPublisher.publishEvent(new UserSignedUpEvent(savedUser.getId()));
        return savedUser;
    }

    // 소셜 로그인
    public User saveSocialUser(String email, String username, SocialProvider provider) {
        User savedUser = createBaseUser(email, null, username, provider, null, null);
        eventPublisher.publishEvent(new UserSignedUpEvent(savedUser.getId()));
        return savedUser;
    }

    // 탈퇴 후 재가입
    public User restoreUser(User user, String username, String password, LocalDate birthDate, String address) {

        user.restore(username, password, birthDate, address);

        if (user.getUserSetting() == null) {
            user.setSettings(
                    UserSetting.builder().user(user).build(),
                    UserNotificationSetting.builder().user(user).build()
            );
        }

        eventPublisher.publishEvent(new UserSignedUpEvent(user.getId()));
        return userRepository.save(user);
    }
}
