package com.chaewookim.accountbookformoms.global.security.oauth2;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class GoogleUserInfoTest {

    @Test
    @DisplayName("구글 정보 매핑 테스트")
    void googleUserInfoTest() {
        Map<String, Object> attr = Map.of("email", "test@google.com", "name", "성은");
        GoogleUserInfo info = new GoogleUserInfo(attr);
        assertThat(info.getEmail()).isEqualTo("test@google.com");
        assertThat(info.getName()).isEqualTo("성은");
    }
}