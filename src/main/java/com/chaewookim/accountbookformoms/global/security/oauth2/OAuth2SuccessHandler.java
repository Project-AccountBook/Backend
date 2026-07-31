package com.chaewookim.accountbookformoms.global.security.oauth2;

import com.chaewookim.accountbookformoms.domain.user.dao.RefreshTokenRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.RefreshToken;
import com.chaewookim.accountbookformoms.global.security.jwt.JwtTokenProvider;
import com.chaewookim.accountbookformoms.global.security.principal.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.chaewookim.accountbookformoms.global.util.CookieUtils;
import jakarta.servlet.http.Cookie;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Value("${app.oauth2.authorized-redirect-uri:http://localhost:5173/oauth2/redirect}")
    private String defaultRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String email = principal.getUsername();
        String role = principal.getAuthorities().iterator().next().getAuthority();

        String accessToken = jwtTokenProvider.createAccessToken(email, role);
        String refreshToken = jwtTokenProvider.createRefreshToken(email);

        refreshTokenRepository.save(new RefreshToken(email, refreshToken));

        Optional<String> redirectUri = CookieUtils.getCookie(request, HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue);

        String targetUrl = redirectUri.orElse(defaultRedirectUri);

        if ("ROLE_ADMIN".equals(role)) {
            if (targetUrl.contains("localhost")) {
                targetUrl = "http://localhost:5174/oauth2/redirect";
            } else {
                targetUrl = "https://admin-frontend-rho-five.vercel.app/oauth2/redirect";
            }
        }

        UriComponentsBuilder redirectBuilder = UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken);

        if (principal.isNewSocialSignup()) {
            redirectBuilder.queryParam("isNewUser", "true");
        }

        targetUrl = redirectBuilder.build().toUriString();

        clearAuthenticationAttributes(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
    }
}
