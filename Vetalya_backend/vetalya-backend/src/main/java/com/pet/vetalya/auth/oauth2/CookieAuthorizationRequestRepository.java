package com.pet.vetalya.auth.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

/**
 * Cookie-based AuthorizationRequestRepository for stateless OAuth2 login.
 * Stores the OAuth2AuthorizationRequest + custom parameters (role, client)
 * in a signed, Base64-encoded cookie.
 *
 * Automatically adapts Secure and SameSite flags:
 * - HTTPS: Secure=true, SameSite=None (supports cross-site form_post for Apple)
 * - HTTP (localhost): Secure=false, SameSite=Lax (ensures cookies are accepted and sent on GET redirects)
 */
public class CookieAuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final Logger log = LoggerFactory.getLogger(CookieAuthorizationRequestRepository.class);
    private static final String COOKIE_NAME = "oauth2_auth_request";
    public static final String ROLE_PARAM = "role";
    public static final String CLIENT_PARAM = "client";
    private static final int COOKIE_MAX_AGE = 300; // 5 minutes
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final byte[] signingKey;

    public CookieAuthorizationRequestRepository(String signingSecret) {
        this.signingKey = signingSecret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return getAuthorizationRequest(request);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeCookie(response, request.isSecure());
            return;
        }

        try {
            var dto = AuthRequestDto.from(
                    authorizationRequest,
                    request.getParameter(ROLE_PARAM),
                    request.getParameter(CLIENT_PARAM)
            );
            String json = MAPPER.writeValueAsString(dto);
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
            String signature = sign(encoded);
            String cookieValue = encoded + "." + signature;

            boolean isSecure = request.isSecure();
            Cookie cookie = new Cookie(COOKIE_NAME, cookieValue);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setSecure(isSecure);
            cookie.setMaxAge(COOKIE_MAX_AGE);
            cookie.setAttribute("SameSite", isSecure ? "None" : "Lax");
            response.addCookie(cookie);
        } catch (Exception e) {
            log.error("Failed to serialize OAuth2 authorization request into cookie", e);
            throw new RuntimeException("Failed to serialize authorization request", e);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                  HttpServletResponse response) {
        OAuth2AuthorizationRequest authRequest = loadAuthorizationRequest(request);
        if (authRequest != null) {
            removeCookie(response, request.isSecure());
        }
        return authRequest;
    }

    /** Extract the role from the cookie (called by the success handler). */
    public String getRole(HttpServletRequest request) {
        AuthRequestDto dto = getDto(request);
        return dto != null ? dto.role : null;
    }

    /** Extract the client type from the cookie (called by the success handler). */
    public String getClientType(HttpServletRequest request) {
        AuthRequestDto dto = getDto(request);
        return dto != null ? dto.client : null;
    }

    private OAuth2AuthorizationRequest getAuthorizationRequest(HttpServletRequest request) {
        AuthRequestDto dto = getDto(request);
        return dto != null ? dto.toOAuth2AuthorizationRequest() : null;
    }

    private AuthRequestDto getDto(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                try {
                    String value = cookie.getValue();
                    int dotIdx = value.lastIndexOf('.');
                    if (dotIdx < 0) {
                        log.warn("Malformed auth request cookie (missing signature separator)");
                        return null;
                    }

                    String encoded = value.substring(0, dotIdx);
                    String signature = value.substring(dotIdx + 1);

                    if (!sign(encoded).equals(signature)) {
                        log.warn("Auth request cookie signature mismatch");
                        return null; // tampered
                    }

                    String json = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
                    return MAPPER.readValue(json, AuthRequestDto.class);
                } catch (Exception e) {
                    log.warn("Failed to deserialize authorization request cookie: {}", e.getMessage());
                    return null;
                }
            }
        }
        return null;
    }

    private void removeCookie(HttpServletResponse response, boolean isSecure) {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setSecure(isSecure);
        cookie.setAttribute("SameSite", isSecure ? "None" : "Lax");
        response.addCookie(cookie);
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
            byte[] sig = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(sig);
        } catch (Exception e) {
            throw new RuntimeException("HMAC signing failed", e);
        }
    }

    /**
     * Clean POJO representation of the OAuth2 authorization request and metadata
     * that deserializes reliably across all Jackson versions without mixins.
     */
    public static class AuthRequestDto {
        public String authorizationUri;
        public String clientId;
        public String redirectUri;
        public Set<String> scopes;
        public String state;
        public Map<String, Object> additionalParameters;
        public String authorizationRequestUri;
        public Map<String, Object> attributes;
        public String role;
        public String client;

        public static AuthRequestDto from(OAuth2AuthorizationRequest req, String role, String client) {
            AuthRequestDto dto = new AuthRequestDto();
            dto.authorizationUri = req.getAuthorizationUri();
            dto.clientId = req.getClientId();
            dto.redirectUri = req.getRedirectUri();
            dto.scopes = req.getScopes();
            dto.state = req.getState();
            dto.additionalParameters = req.getAdditionalParameters();
            dto.authorizationRequestUri = req.getAuthorizationRequestUri();
            dto.attributes = req.getAttributes();
            dto.role = role;
            dto.client = client;
            return dto;
        }

        public OAuth2AuthorizationRequest toOAuth2AuthorizationRequest() {
            return OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri(this.authorizationUri)
                    .clientId(this.clientId)
                    .redirectUri(this.redirectUri)
                    .scopes(this.scopes)
                    .state(this.state)
                    .additionalParameters(this.additionalParameters != null ? this.additionalParameters : Map.of())
                    .authorizationRequestUri(this.authorizationRequestUri)
                    .attributes(a -> { if (this.attributes != null) a.putAll(this.attributes); })
                    .build();
        }
    }
}
