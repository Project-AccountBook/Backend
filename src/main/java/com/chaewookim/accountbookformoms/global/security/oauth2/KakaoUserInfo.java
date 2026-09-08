package com.chaewookim.accountbookformoms.global.security.oauth2;

import java.util.Map;

public record KakaoUserInfo(Map<String, Object> attributes) implements OAuth2UserInfo {

    @Override
    public String getProvider() { return "kakao"; }

    @Override
    @SuppressWarnings("unchecked")
    public String getEmail() {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        return (String) account.get("email");
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getName() {
        Map<String, Object> profile = (Map<String, Object>) attributes.get("properties");
        return (String) profile.get("nickname");
    }
}
