package com.pet.vetalya.auth.oauth2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.vetalya.auth.mapper.AppleUserMapper;
import com.pet.vetalya.auth.mapper.GoogleUserMapper;
import com.pet.vetalya.auth.service.SocialLoginService;
import com.pet.vetalya.auth.service.TokenService;
import com.pet.vetalya.config.AppProperties;
import com.pet.vetalya.dto.ProviderUserInfo;
import com.pet.vetalya.entity.OneTimeCode;
import com.pet.vetalya.repository.OneTimeCodeRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

/**
 * After successful OAuth2 login, maps the provider principal to ProviderUserInfo,
 * runs find-or-create via SocialLoginService, and delivers tokens:
 *
 * - Web: refresh token in HttpOnly cookie, redirect to web app URL
 * - Mobile (android/ios): generate a one-time code, redirect to deep link with the code
 */
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CookieAuthorizationRequestRepository authRequestRepo;
    private final GoogleUserMapper googleMapper;
    private final AppleUserMapper appleMapper;
    private final SocialLoginService socialLoginService;
    private final AppProperties appProperties;
    private final OneTimeCodeRepository oneTimeCodeRepo;

    public OAuth2LoginSuccessHandler(CookieAuthorizationRequestRepository authRequestRepo,
                                     GoogleUserMapper googleMapper,
                                     AppleUserMapper appleMapper,
                                     SocialLoginService socialLoginService,
                                     AppProperties appProperties,
                                     OneTimeCodeRepository oneTimeCodeRepo) {
        this.authRequestRepo = authRequestRepo;
        this.googleMapper = googleMapper;
        this.appleMapper = appleMapper;
        this.socialLoginService = socialLoginService;
        this.appProperties = appProperties;
        this.oneTimeCodeRepo = oneTimeCodeRepo;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();

        String role = authRequestRepo.getRole(request);
        String clientType = authRequestRepo.getClientType(request);

        if (role == null) role = "ROLE_PET_OWNER"; // safe default
        if (clientType == null) clientType = "web";

        // Map provider-specific principal to neutral ProviderUserInfo
        ProviderUserInfo info;
        if ("apple".equals(registrationId)) {
            // Apple sends user name only on first authorization, in form_post "user" param
            String firstName = null;
            String lastName = null;
            String userParam = request.getParameter("user");
            if (userParam != null) {
                try {
                    JsonNode userNode = MAPPER.readTree(userParam);
                    JsonNode nameNode = userNode.path("name");
                    firstName = nameNode.path("firstName").asText(null);
                    lastName = nameNode.path("lastName").asText(null);
                } catch (Exception e) {
                    log.warn("Failed to parse Apple user param: {}", e.getMessage());
                }
            }
            info = appleMapper.map(oidcUser, firstName, lastName);
        } else {
            info = googleMapper.map(oidcUser);
        }

        String ipAddress = request.getRemoteAddr();

        try {
            SocialLoginService.LoginResult result = socialLoginService.processLogin(
                    info, role, clientType, null, ipAddress);

            deliverTokens(request, response, result, clientType);

        } catch (SocialLoginService.AccountBlockedException e) {
            redirectWithError(response, clientType, "account_blocked");
        } catch (IllegalArgumentException e) {
            redirectWithError(response, clientType, "invalid_role");
        } catch (Exception e) {
            log.error("OAuth2 login failed", e);
            redirectWithError(response, clientType, "server_error");
        }
    }

    private void deliverTokens(HttpServletRequest request, HttpServletResponse response,
                                SocialLoginService.LoginResult result, String clientType) throws IOException {
        String code = generateOneTimeCode();

        OneTimeCode otc = new OneTimeCode();
        otc.setCodeHash(TokenService.sha256(code));
        otc.setUserId(result.user().getUserId());
        otc.setClientType(clientType);
        otc.setNewUser(result.newUser());
        otc.setProfileComplete(result.profileComplete());
        otc.setExpiresAt(Instant.now().plus(60, ChronoUnit.SECONDS));
        oneTimeCodeRepo.save(otc);

        if ("web".equals(clientType) || clientType == null) {
            // Set refresh token as HttpOnly cookie with local-friendly attributes
            boolean isSecure = request.isSecure();
            ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", result.refreshToken())
                    .httpOnly(true)
                    .secure(isSecure)
                    .path("/")
                    .maxAge(appProperties.jwt().refreshTokenExpiryDays() * 24L * 60 * 60)
                    .sameSite(isSecure ? "None" : "Lax")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

            // Redirect to web app with one-time code for instant zero-collision exchange
            String redirectUrl = appProperties.webRedirectUrl()
                    + "/auth/callback?code=" + code
                    + "&newUser=" + result.newUser()
                    + "&profileComplete=" + result.profileComplete();
            response.sendRedirect(redirectUrl);
        } else {
            // Mobile: redirect to deep link with code
            String deepLink = appProperties.mobileRedirectScheme()
                    + "://auth/callback?code=" + code
                    + "&newUser=" + result.newUser()
                    + "&profileComplete=" + result.profileComplete();
            response.sendRedirect(deepLink);
        }
    }

    private void redirectWithError(HttpServletResponse response, String clientType, String errorCode) throws IOException {
        if ("web".equals(clientType) || clientType == null) {
            response.sendRedirect(appProperties.webRedirectUrl() + "/auth/error?code=" + errorCode);
        } else {
            response.sendRedirect(appProperties.mobileRedirectScheme() + "://auth/error?code=" + errorCode);
        }
    }

    private String generateOneTimeCode() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
