package com.chaewookim.accountbookformoms.domain.user.application;

import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.domain.user.entity.UserNotificationSetting;
import com.chaewookim.accountbookformoms.domain.user.entity.UserSetting;
import com.chaewookim.accountbookformoms.domain.user.enums.SocialProvider;
import com.chaewookim.accountbookformoms.global.event.UserSignedUpEvent;
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
    public User saveUser(String email, String password, String username, SocialProvider provider, LocalDate birthDate, String address) {
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

        User savedUser = userRepository.save(user);
        eventPublisher.publishEvent(new UserSignedUpEvent(savedUser.getId()));
        return savedUser;
    }

    // 소셜 로그인
    public User saveSocialUser(String email, String username, SocialProvider provider) {
        return saveUser(email, null, username, provider, null, null);
    }
}
