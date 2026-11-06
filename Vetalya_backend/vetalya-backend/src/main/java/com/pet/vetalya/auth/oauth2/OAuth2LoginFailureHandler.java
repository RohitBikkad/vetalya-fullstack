package com.pet.vetalya.auth.oauth2;

import com.pet.vetalya.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;

/**
 * On OAuth2 login failure (including user cancellation at the provider),
 * redirects to web or mobile app with a generic error code. No stack traces.
 */
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);

    private final AppProperties appProperties;
    private final CookieAuthorizationRequestRepository authRequestRepo;

    public OAuth2LoginFailureHandler(AppProperties appProperties,
                                      CookieAuthorizationRequestRepository authRequestRepo) {
        this.appProperties = appProperties;
        this.authRequestRepo = authRequestRepo;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.warn("OAuth2 login failed: {}", exception.getMessage());

        String clientType = authRequestRepo.getClientType(request);

        if ("android".equals(clientType) || "ios".equals(clientType)) {
            response.sendRedirect(appProperties.mobileRedirectScheme() + "://auth/error?code=login_failed");
        } else {
            response.sendRedirect(appProperties.webRedirectUrl() + "/auth/error?code=login_failed");
        }
    }
}
