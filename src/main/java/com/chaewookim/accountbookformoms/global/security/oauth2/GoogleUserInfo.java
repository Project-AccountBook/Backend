package com.chaewookim.accountbookformoms.global.security.oauth2;

import java.util.Map;

public record GoogleUserInfo(Map<String, Object> attributes) implements OAuth2UserInfo {

    @Override
    public String getProvider() { return "google"; }

    @Override
    public String getEmail(){ return (String) attributes.get("email"); }

    @Override
    public String getName() { return (String) attributes.get("name"); }
}
