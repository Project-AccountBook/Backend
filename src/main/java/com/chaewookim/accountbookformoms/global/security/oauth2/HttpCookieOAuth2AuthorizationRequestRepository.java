package com.chaewookim.accountbookformoms.global.security.oauth2;

import com.chaewookim.accountbookformoms.global.util.CookieUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class HttpCookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String POST_LOGIN_REDIRECT_PARAM = "post_login_redirect";
    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    public static final String POST_LOGIN_REDIRECT_COOKIE = "post_login_redirect";

    private static final int cookieExpireSeconds = 180;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return CookieUtils.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                .map(cookie -> CookieUtils.deserialize(cookie, OAuth2AuthorizationRequest.class))
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            CookieUtils.deleteCookie(request, response, POST_LOGIN_REDIRECT_COOKIE);
            CookieUtils.deleteCookie(request, response, "redirect_uri");
            return;
        }

        CookieUtils.deleteCookie(request, response, "redirect_uri");

        String redirectUriAfterLogin = request.getParameter(POST_LOGIN_REDIRECT_PARAM);
        OAuth2AuthorizationRequest toSave = authorizationRequest;

        if (redirectUriAfterLogin != null && !redirectUriAfterLogin.isBlank()) {
            Map<String, Object> additionalParameters = new HashMap<>(authorizationRequest.getAdditionalParameters());
            additionalParameters.put(POST_LOGIN_REDIRECT_COOKIE, redirectUriAfterLogin);
            toSave = OAuth2AuthorizationRequest.from(authorizationRequest)
                    .additionalParameters(additionalParameters)
                    .build();
            CookieUtils.addCookie(response, POST_LOGIN_REDIRECT_COOKIE, redirectUriAfterLogin, cookieExpireSeconds);
        }

        CookieUtils.addCookie(
                response,
                OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME,
                CookieUtils.serialize(toSave),
                cookieExpireSeconds
        );
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        return this.loadAuthorizationRequest(request);
    }

    public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        CookieUtils.deleteCookie(request, response, POST_LOGIN_REDIRECT_COOKIE);
        CookieUtils.deleteCookie(request, response, "redirect_uri");
    }

    public String resolveRedirectUriAfterLogin(HttpServletRequest request) {
        String fromCookie = CookieUtils.getCookie(request, POST_LOGIN_REDIRECT_COOKIE)
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .orElse(null);
        if (fromCookie != null) {
            return fromCookie;
        }

        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        if (authorizationRequest == null) {
            return null;
        }

        Object embedded = authorizationRequest.getAdditionalParameters().get(POST_LOGIN_REDIRECT_COOKIE);
        if (embedded == null) {
            return null;
        }
        String value = embedded.toString();
        return value.isBlank() ? null : value;
    }
}
