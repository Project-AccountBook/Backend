package com.chaewookim.accountbookformoms.global.security.oauth2;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class KakaoUserInfoTest {

    @Test
    @DisplayName("카카오 정보 매핑 테스트")
    void kakaoUserInfoTest() {
        Map<String, Object> attr = Map.of(
                "kakao_account", Map.of("email", "test@kakao.com"),
                "properties", Map.of("nickname", "성은")
        );
        KakaoUserInfo info = new KakaoUserInfo(attr);
        assertThat(info.getEmail()).isEqualTo("test@kakao.com");
        assertThat(info.getName()).isEqualTo("성은");
    }
}