package com.chaewookim.accountbookformoms.global.security.oauth2;

import com.chaewookim.accountbookformoms.domain.user.dao.RefreshTokenRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.RefreshToken;
import com.chaewookim.accountbookformoms.global.security.jwt.JwtTokenProvider;
import com.chaewookim.accountbookformoms.global.security.principal.UserPrincipal;
import com.chaewookim.accountbookformoms.global.util.CookieUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    static final String OAUTH_CODE_KEY_PREFIX = "OAUTH2:CODE:";
    private static final Duration OAUTH_CODE_TTL = Duration.ofSeconds(60);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;
    private final OAuth2RedirectUriValidator redirectUriValidator;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.oauth2.admin-redirect-uri:https://admin-frontend-rho-five.vercel.app/oauth2/redirect}")
    private String adminRedirectUri;

    @Value("${app.oauth2.admin-redirect-uri-local:http://localhost:5174/oauth2/redirect}")
    private String adminRedirectUriLocal;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String email = principal.getUsername();
        String role = principal.getAuthorities().iterator().next().getAuthority();

        String accessToken = jwtTokenProvider.createAccessToken(email, role);
        String refreshToken = jwtTokenProvider.createRefreshToken(email);
        refreshTokenRepository.save(new RefreshToken(email, refreshToken));

        // AuthService를 주입하면 SecurityConfig ↔ AuthService 순환참조가 생기므로 여기서 직접 발급
        String code = issueOAuthCode(accessToken, refreshToken, principal.isNewSocialSignup());

        String candidateUri = CookieUtils.getCookie(request, HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue)
                .orElse(null);

        String targetUrl = redirectUriValidator.resolve(candidateUri);

        if ("ROLE_ADMIN".equals(role)) {
            targetUrl = targetUrl.contains("localhost") ? adminRedirectUriLocal : adminRedirectUri;
        }

        targetUrl = UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("code", code)
                .build()
                .toUriString();

        clearAuthenticationAttributes(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String issueOAuthCode(String accessToken, String refreshToken, boolean isNewUser) {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String code = HexFormat.of().formatHex(bytes);
        String value = accessToken + "\n" + refreshToken + "\n" + isNewUser;
        redisTemplate.opsForValue().set(OAUTH_CODE_KEY_PREFIX + code, value, OAUTH_CODE_TTL);
        return code;
    }

    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
    }
}
