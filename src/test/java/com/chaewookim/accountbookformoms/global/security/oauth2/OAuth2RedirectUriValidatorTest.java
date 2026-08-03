package com.chaewookim.accountbookformoms.global.security.oauth2;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2RedirectUriValidatorTest {

    private static final String DEFAULT_URI = "http://localhost:5173/oauth2/redirect";
    private static final List<String> ALLOWED = List.of(
            DEFAULT_URI,
            "https://moneydiary.cloud/oauth2/redirect",
            "com.jointliving.app://oauth2/redirect"
    );

    private final OAuth2RedirectUriValidator validator = new OAuth2RedirectUriValidator(ALLOWED, DEFAULT_URI);

    @Test
    @DisplayName("화이트리스트에 있는 URI 는 그대로 반환한다")
    void allowsListedUri() {
        assertThat(validator.resolve("com.jointliving.app://oauth2/redirect"))
                .isEqualTo("com.jointliving.app://oauth2/redirect");
        assertThat(validator.resolve("https://moneydiary.cloud/oauth2/redirect"))
                .isEqualTo("https://moneydiary.cloud/oauth2/redirect");
    }

    @Test
    @DisplayName("화이트리스트에 없는 URI 는 기본 URI 로 fallback 한다")
    void rejectsUnknownUri() {
        assertThat(validator.resolve("https://evil.com/steal"))
                .isEqualTo(DEFAULT_URI);
    }

    @Test
    @DisplayName("null 또는 빈 값은 기본 URI 로 fallback 한다")
    void fallbacksOnEmpty() {
        assertThat(validator.resolve(null)).isEqualTo(DEFAULT_URI);
        assertThat(validator.resolve("")).isEqualTo(DEFAULT_URI);
        assertThat(validator.resolve("   ")).isEqualTo(DEFAULT_URI);
    }

    @Test
    @DisplayName("URI 는 정확 일치 검증한다 (prefix / suffix 로는 통과 불가)")
    void rejectsPartialMatch() {
        assertThat(validator.resolve("https://moneydiary.cloud/oauth2/redirect?extra=1"))
                .isEqualTo(DEFAULT_URI);
        assertThat(validator.resolve("https://moneydiary.cloud.evil.com/oauth2/redirect"))
                .isEqualTo(DEFAULT_URI);
    }
}
