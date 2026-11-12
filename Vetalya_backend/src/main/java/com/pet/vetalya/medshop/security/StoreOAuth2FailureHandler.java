package com.pet.vetalya.medshop.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class StoreOAuth2FailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(StoreOAuth2FailureHandler.class);

    private final StoreCookieAuthRequestRepo cookieRepo;

    @Value("${app.store.web-redirect-url:http://localhost:3000/store/auth/callback}")
    private String webRedirectUrl;

    @Value("${app.store.mobile-redirect-scheme:medshop://auth/callback}")
    private String mobileRedirectScheme;

    public StoreOAuth2FailureHandler(StoreCookieAuthRequestRepo cookieRepo) {
        this.cookieRepo = cookieRepo;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.warn("OAuth2 store authentication failed: {}", exception.getMessage());
        String clientType = cookieRepo.getAndClearClientType(request, response);

        String targetUrl;
        if ("android".equalsIgnoreCase(clientType) || "ios".equalsIgnoreCase(clientType)) {
            targetUrl = mobileRedirectScheme + "?error=auth_failed";
        } else {
            targetUrl = webRedirectUrl + "?error=auth_failed";
        }

        response.sendRedirect(targetUrl);
    }
}

