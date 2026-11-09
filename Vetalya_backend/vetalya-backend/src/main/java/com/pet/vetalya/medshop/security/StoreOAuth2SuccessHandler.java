package com.pet.vetalya.medshop.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.vetalya.medshop.auth.dto.AuthDtos.TokenResponse;
import com.pet.vetalya.medshop.auth.service.StoreAuthService;
import com.pet.vetalya.medshop.auth.service.StoreSocialLoginService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class StoreOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(StoreOAuth2SuccessHandler.class);

    private final StoreCookieAuthRequestRepo cookieRepo;
    private final StoreSocialLoginService socialLoginService;
    private final StoreAuthService authService;
    private final ObjectMapper objectMapper;

    @Value("${app.store.web-redirect-url:http://localhost:3000/store/auth/callback}")
    private String webRedirectUrl;

    @Value("${app.store.mobile-redirect-scheme:medshop://auth/callback}")
    private String mobileRedirectScheme;

    public StoreOAuth2SuccessHandler(
            StoreCookieAuthRequestRepo cookieRepo,
            StoreSocialLoginService socialLoginService,
            StoreAuthService authService,
            ObjectMapper objectMapper) {
        this.cookieRepo = cookieRepo;
        this.socialLoginService = socialLoginService;
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
        String registrationId = authToken.getAuthorizedClientRegistrationId();
        OAuth2User principal = authToken.getPrincipal();

        String provider;
        String sub;
        String email;
        boolean emailVerified;
        String name;

        if (registrationId.contains("google")) {
            provider = "GOOGLE";
            sub = principal.getAttribute("sub");
            email = principal.getAttribute("email");
            Object verifiedObj = principal.getAttribute("email_verified");
            emailVerified = Boolean.TRUE.equals(verifiedObj) || "true".equalsIgnoreCase(String.valueOf(verifiedObj));
            name = principal.getAttribute("name");
        } else if (registrationId.contains("apple")) {
            provider = "APPLE";
            sub = principal.getAttribute("sub");
            email = principal.getAttribute("email");
            Object verifiedObj = principal.getAttribute("email_verified");
            emailVerified = Boolean.TRUE.equals(verifiedObj) || "true".equalsIgnoreCase(String.valueOf(verifiedObj));

            // Apple sends the user's name ONLY on the first authorization in POST parameter "user"
            String userJson = request.getParameter("user");
            name = parseAppleName(userJson);
        } else {
            provider = registrationId.toUpperCase();
            sub = principal.getName();
            email = principal.getAttribute("email");
            emailVerified = false;
            name = principal.getAttribute("name");
        }

        String clientType = cookieRepo.getAndClearClientType(request, response);
        String ipAddress = request.getRemoteAddr();

        TokenResponse result = socialLoginService.processSocialLogin(
                provider, sub, email, emailVerified, name, clientType, null, ipAddress);

        // One-time exchange code (never put tokens directly in URL redirect)
        String exchangeCode = authService.createExchangeCode(result);

        // If web client and tokens were issued, set refresh token cookie
        if ("web".equalsIgnoreCase(clientType) && result.refreshToken() != null) {
            Cookie refreshCookie = new Cookie("store_refresh_token", result.refreshToken());
            refreshCookie.setPath("/api/store/auth");
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(true);
            refreshCookie.setMaxAge(14 * 24 * 60 * 60);
            response.addCookie(refreshCookie);
        }

        String targetUrl;
        if ("android".equalsIgnoreCase(clientType) || "ios".equalsIgnoreCase(clientType)) {
            targetUrl = mobileRedirectScheme + "?code=" + URLEncoder.encode(exchangeCode, StandardCharsets.UTF_8);
        } else {
            targetUrl = webRedirectUrl + "?code=" + URLEncoder.encode(exchangeCode, StandardCharsets.UTF_8);
        }

        response.sendRedirect(targetUrl);
    }

    @SuppressWarnings("unchecked")
    private String parseAppleName(String userJson) {
        if (userJson == null || userJson.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> map = objectMapper.readValue(userJson, Map.class);
            Map<String, Object> nameMap = (Map<String, Object>) map.get("name");
            if (nameMap != null) {
                String first = (String) nameMap.get("firstName");
                String last = (String) nameMap.get("lastName");
                if (first != null && last != null) return first + " " + last;
                if (first != null) return first;
                return last;
            }
        } catch (Exception e) {
            log.warn("Failed to parse Apple user JSON: {}", e.getMessage());
        }
        return null;
    }
}

