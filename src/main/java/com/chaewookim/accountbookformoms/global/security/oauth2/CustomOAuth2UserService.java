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
import org.springframework.transaction.annotation.Transactional;
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
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserInfo userInfo = switch (registrationId) {
            case "google" -> new GoogleUserInfo(oAuth2User.getAttributes());
            case "kakao" -> new KakaoUserInfo(oAuth2User.getAttributes());
            case "naver" -> new NaverUserInfo(oAuth2User.getAttributes());
            default -> throw new CustomException(UserErrorCode.UNSUPPORTED_SOCIAL_TYPE);
        };

        var existingUser = userRepository.findByEmailIncludingDeleted(userInfo.getEmail());
        boolean newSocialSignup;
        User user;

        if (existingUser.isPresent()) {
            User entity = existingUser.get();
            if (entity.getDeletedAt() != null) {
                user = userCommonService.restoreUser(entity, userInfo.getName(), null, null, null);
                newSocialSignup = true;
            } else {
                user = entity.update(userInfo.getName());
                newSocialSignup = false;
            }
        } else {
            user = userCommonService.saveSocialUser(
                    userInfo.getEmail(),
                    userInfo.getName(),
                    SocialProvider.from(userInfo.getProvider())
            );
            newSocialSignup = true;
        }

        if (adminEmail.equals(userInfo.getEmail())) {
            user.updateRole(com.chaewookim.accountbookformoms.domain.user.enums.UserRole.ROLE_ADMIN);
            userRepository.save(user);
        }

        return UserPrincipal.create(user, oAuth2User.getAttributes(), newSocialSignup);
    }
}
