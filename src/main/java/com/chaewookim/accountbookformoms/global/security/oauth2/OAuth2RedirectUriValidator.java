package com.chaewookim.accountbookformoms.global.security.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class OAuth2RedirectUriValidator {

    private final Set<String> allowedRedirectUris;
    private final String defaultRedirectUri;

    public OAuth2RedirectUriValidator(
            @Value("${app.oauth2.authorized-redirect-uris:}") List<String> allowedRedirectUris,
            @Value("${app.oauth2.authorized-redirect-uri:com.jointliving.app://oauth2/redirect}") String defaultRedirectUri
    ) {
        this.allowedRedirectUris = Set.copyOf(allowedRedirectUris);
        this.defaultRedirectUri = defaultRedirectUri;
    }

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
