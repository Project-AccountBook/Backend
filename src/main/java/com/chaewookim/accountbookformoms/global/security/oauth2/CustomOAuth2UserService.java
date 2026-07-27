package com.chaewookim.accountbookformoms.global.security.oauth2;

import com.chaewookim.accountbookformoms.domain.user.application.UserCommonService;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.domain.user.enums.SocialProvider;
import com.chaewookim.accountbookformoms.domain.user.error.UserErrorCode;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final UserCommonService userCommonService;

    @org.springframework.beans.factory.annotation.Value("${app.admin.email:modi091321@gmail.com}")
    private String adminEmail;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserInfo userInfo = switch (registrationId) {
            case "google" -> new GoogleUserInfo(oAuth2User.getAttributes());
            case "kakao" -> new KakaoUserInfo(oAuth2User.getAttributes());
            case "naver" -> new NaverUserInfo(oAuth2User.getAttributes());
            default -> throw new CustomException(UserErrorCode.UNSUPPORTED_SOCIAL_TYPE);
        };

        User user = userRepository.findByEmailIncludingDeleted(userInfo.getEmail())
                .map(entity -> {
                    if (entity.getDeletedAt() != null) {
                        return userCommonService.restoreUser(entity, userInfo.getName(), null, null, null);
                    }
                    return entity.update(userInfo.getName());
                })
                .orElseGet(() -> userCommonService.saveSocialUser(
                        userInfo.getEmail(),
                        userInfo.getName(),
                        SocialProvider.from(userInfo.getProvider())
                ));

        if (adminEmail.equals(userInfo.getEmail())) {
            user.updateRole(com.chaewookim.accountbookformoms.domain.user.enums.UserRole.ROLE_ADMIN);
            userRepository.save(user);
        }

        return UserPrincipal.create(user, oAuth2User.getAttributes());
    }
}
