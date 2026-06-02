package com.chaewookim.accountbookformoms.global.security.oauth2;

import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.domain.user.enums.SocialProvider;
import com.chaewookim.accountbookformoms.domain.user.enums.UserRole;
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

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserInfo userInfo = switch (registrationId) {
            case "google" -> new GoogleUserInfo(oAuth2User.getAttributes());
            case "kakao" -> new KakaoUserInfo(oAuth2User.getAttributes());
            case "naver" -> new NaverUserInfo(oAuth2User.getAttributes());
            default -> throw new RuntimeException("지원하지 않는 소셜 로그인입니다.");
        };

        User user = userRepository.findByEmail(userInfo.getEmail())
                .map(entity -> entity.update(userInfo.getName()))
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(userInfo.getEmail())
                        .username(userInfo.getName())
                        .role(UserRole.ROLE_USER)
                        .provider(SocialProvider.from(userInfo.getProvider()))
                        .build()));

        return UserPrincipal.create(user, oAuth2User.getAttributes());
    }
}
