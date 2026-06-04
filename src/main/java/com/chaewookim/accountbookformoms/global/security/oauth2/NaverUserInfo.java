package com.chaewookim.accountbookformoms.global.security.oauth2;

import java.util.Map;

public record NaverUserInfo(Map<String, Object> attributes) implements OAuth2UserInfo {

    @Override
    public String getProvider() { return "naver"; }

    @Override
    @SuppressWarnings("unchecked")
    public String getEmail() {
        return (String) ((Map<String, Object>) attributes.get("response")).get("email");
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getName() {
        return (String) ((Map<String, Object>) attributes.get("response")).get("name");
    }
}
