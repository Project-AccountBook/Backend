package com.chaewookim.accountbookformoms.global.security.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * OAuth2 로그인 완료 후 리다이렉트할 URI 를 화이트리스트 검증한다.
 * 화이트리스트에 없는 URI 는 open redirect 로 악용될 수 있으므로 기본 URI 로 fallback 한다.
 */
@Slf4j
@Component
public class OAuth2RedirectUriValidator {

    private final Set<String> allowedRedirectUris;
    private final String defaultRedirectUri;

    public OAuth2RedirectUriValidator(
            @Value("${app.oauth2.authorized-redirect-uris:}") List<String> allowedRedirectUris,
            @Value("${app.oauth2.authorized-redirect-uri:http://localhost:5173/oauth2/redirect}") String defaultRedirectUri
    ) {
        this.allowedRedirectUris = Set.copyOf(allowedRedirectUris);
        this.defaultRedirectUri = defaultRedirectUri;
    }

    /**
     * 주어진 URI 가 화이트리스트에 있으면 그대로 반환, 아니면 기본 URI 로 fallback.
     * fallback 발생 시 WARN 로그 남김 (공격 시도 탐지 용도).
     */
    public String resolve(String candidateUri) {
        if (candidateUri == null || candidateUri.isBlank()) {
            return defaultRedirectUri;
        }
        if (allowedRedirectUris.contains(candidateUri)) {
            return candidateUri;
        }
        log.warn("OAuth2 redirect_uri whitelist 미등록 값 감지, 기본 URI 로 fallback: {}", candidateUri);
        return defaultRedirectUri;
    }
}
