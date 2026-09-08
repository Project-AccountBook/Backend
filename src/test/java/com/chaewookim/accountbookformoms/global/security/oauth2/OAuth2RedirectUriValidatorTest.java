package com.chaewookim.accountbookformoms.global.security.oauth2;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2RedirectUriValidatorTest {

    private static final String APP_DEFAULT_URI = "com.jointliving.app://oauth2/redirect";
    private static final String WEB_DEFAULT_URI = "http://localhost:5173/oauth2/redirect";
    private static final List<String> ALLOWED = List.of(
            WEB_DEFAULT_URI,
            "https://moneydiary.cloud/oauth2/redirect",
            APP_DEFAULT_URI
    );

    private final OAuth2RedirectUriValidator validator =
            new OAuth2RedirectUriValidator(ALLOWED, APP_DEFAULT_URI, WEB_DEFAULT_URI);

    @Test
    @DisplayName("화이트리스트에 있는 URI 는 그대로 반환한다")
    void allowsListedUri() {
        assertThat(validator.resolve(APP_DEFAULT_URI)).isEqualTo(APP_DEFAULT_URI);
        assertThat(validator.resolve("https://moneydiary.cloud/oauth2/redirect"))
                .isEqualTo("https://moneydiary.cloud/oauth2/redirect");
    }

    @Test
    @DisplayName("알 수 없는 http(s) URI 는 웹 기본값으로 fallback 한다")
    void rejectsUnknownHttpUri() {
        assertThat(validator.resolve("https://evil.com/steal")).isEqualTo(WEB_DEFAULT_URI);
    }

    @Test
    @DisplayName("Capacitor WebView 오리진은 앱 딥링크로 fallback 한다")
    void capacitorMisredirectUsesAppDefault() {
        assertThat(validator.resolve("https://localhost/oauth2/redirect")).isEqualTo(APP_DEFAULT_URI);
    }

    @Test
    @DisplayName("null 또는 빈 값은 앱 기본값으로 fallback 한다")
    void fallbacksOnEmpty() {
        assertThat(validator.resolve(null)).isEqualTo(APP_DEFAULT_URI);
        assertThat(validator.resolve("")).isEqualTo(APP_DEFAULT_URI);
        assertThat(validator.resolve("   ")).isEqualTo(APP_DEFAULT_URI);
    }

    @Test
    @DisplayName("URI 는 정확 일치 검증한다 (prefix / suffix 로는 통과 불가)")
    void rejectsPartialMatch() {
        assertThat(validator.resolve("https://moneydiary.cloud/oauth2/redirect?extra=1"))
                .isEqualTo(WEB_DEFAULT_URI);
        assertThat(validator.resolve("https://moneydiary.cloud.evil.com/oauth2/redirect"))
                .isEqualTo(WEB_DEFAULT_URI);
    }
}
