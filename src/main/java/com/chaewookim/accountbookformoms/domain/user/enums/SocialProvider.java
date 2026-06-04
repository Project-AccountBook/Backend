package com.chaewookim.accountbookformoms.domain.user.enums;

public enum SocialProvider {

    LOCAL,
    GOOGLE,
    KAKAO,
    NAVER;

    public static SocialProvider from(String provider) {
        return SocialProvider.valueOf(provider.toUpperCase());
    }
}
