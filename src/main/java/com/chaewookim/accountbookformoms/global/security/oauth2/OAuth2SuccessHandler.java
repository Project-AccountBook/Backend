package com.chaewookim.accountbookformoms.global.security.oauth2;

import com.chaewookim.accountbookformoms.domain.user.application.AuthService;
import com.chaewookim.accountbookformoms.domain.user.dto.response.TokenResponse;
import com.chaewookim.accountbookformoms.global.security.principal.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.chaewookim.accountbookformoms.global.util.CookieUtils;
import jakarta.servlet.http.Cookie;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;
    private final OAuth2RedirectUriValidator redirectUriValidator;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String email = principal.getUsername();
        String role = principal.getAuthorities().iterator().next().getAuthority();

        TokenResponse tokens = authService.issueTokens(email, role);

        String candidateUri = CookieUtils.getCookie(request, HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue)
                .orElse(null);

        String targetUrl = redirectUriValidator.resolve(candidateUri);

        if ("ROLE_ADMIN".equals(role)) {
            if (targetUrl.contains("localhost")) {
                targetUrl = "http://localhost:5174/oauth2/redirect";
            } else {
                targetUrl = "https://admin-frontend-rho-five.vercel.app/oauth2/redirect";
            }
        }

        UriComponentsBuilder redirectBuilder = UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("accessToken", tokens.accessToken())
                .queryParam("refreshToken", tokens.refreshToken());

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
