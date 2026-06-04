package com.chaewookim.accountbookformoms.global.security.oauth2;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class NaverUserInfoTest {

    @Test
    @DisplayName("네이버 정보 매핑 테스트")
    void naverUserInfoTest() {
        Map<String, Object> attr = Map.of("response", Map.of("email", "test@naver.com", "name", "성은"));
        NaverUserInfo info = new NaverUserInfo(attr);
        assertThat(info.getEmail()).isEqualTo("test@naver.com");
        assertThat(info.getName()).isEqualTo("성은");
    }
}