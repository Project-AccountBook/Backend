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
    private final String appDefaultRedirectUri;
    private final String webDefaultRedirectUri;

    public OAuth2RedirectUriValidator(
            @Value("${app.oauth2.authorized-redirect-uris:}") List<String> allowedRedirectUris,
            @Value("${app.oauth2.authorized-redirect-uri:com.jointliving.app://oauth2/redirect}") String appDefaultRedirectUri,
            @Value("${app.oauth2.authorized-redirect-uri-web:http://localhost:5173/oauth2/redirect}") String webDefaultRedirectUri
    ) {
        this.allowedRedirectUris = Set.copyOf(allowedRedirectUris);
        this.appDefaultRedirectUri = appDefaultRedirectUri;
        this.webDefaultRedirectUri = webDefaultRedirectUri;
    }

    public String resolve(String candidateUri) {
        if (candidateUri != null && !candidateUri.isBlank() && allowedRedirectUris.contains(candidateUri)) {
            return candidateUri;
        }
        if (candidateUri != null && !candidateUri.isBlank()) {
            log.warn("OAuth2 redirect_uri whitelist 미등록 값 감지, fallback 적용: {}", candidateUri);
            return fallbackForUnknown(candidateUri);
        }
        return appDefaultRedirectUri;
    }

    private String fallbackForUnknown(String candidateUri) {
        if (isCapacitorMisredirect(candidateUri)) {
            return appDefaultRedirectUri;
        }
        if (candidateUri.startsWith("http://") || candidateUri.startsWith("https://")) {
            return webDefaultRedirectUri;
        }
        return appDefaultRedirectUri;
    }

    private static boolean isCapacitorMisredirect(String candidateUri) {
        return candidateUri.startsWith("https://localhost/")
                || candidateUri.startsWith("capacitor://")
                || candidateUri.startsWith("http://localhost/")
                        && !candidateUri.startsWith("http://localhost:5173/")
                        && !candidateUri.startsWith("http://localhost:5174/");
    }
}
