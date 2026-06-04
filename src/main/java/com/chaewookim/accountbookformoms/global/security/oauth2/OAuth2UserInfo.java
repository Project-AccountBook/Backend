package com.chaewookim.accountbookformoms.global.security.oauth2;

public interface OAuth2UserInfo {

    String getProvider();
    String getEmail();
    String getName();
}
