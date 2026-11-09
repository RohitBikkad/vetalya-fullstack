package com.pet.vetalya.auth.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Cookie-based AuthorizationRequestRepository for stateless OAuth2 login.
 * Stores the OAuth2AuthorizationRequest + custom parameters (role, client)
 * in a signed, Base64-encoded cookie.
 *
 * Cookies are SameSite=None; Secure to support Apple's cross-site form_post callback.
 * The cookie is signed with HMAC-SHA256 to prevent tampering.
 */
public class CookieAuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

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
        return getCookieValue(request);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeCookie(response);
            return;
        }

        try {
            // Build a wrapper that includes role and client from the original request
            var wrapper = new AuthRequestWrapper(
                    authorizationRequest,
                    request.getParameter(ROLE_PARAM),
                    request.getParameter(CLIENT_PARAM)
            );
            String json = MAPPER.writeValueAsString(wrapper);
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
            String signature = sign(encoded);
            String cookieValue = encoded + "." + signature;

            Cookie cookie = new Cookie(COOKIE_NAME, cookieValue);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setMaxAge(COOKIE_MAX_AGE);
            cookie.setAttribute("SameSite", "None");
            response.addCookie(cookie);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize authorization request", e);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                  HttpServletResponse response) {
        OAuth2AuthorizationRequest authRequest = loadAuthorizationRequest(request);
        if (authRequest != null) {
            removeCookie(response);
        }
        return authRequest;
    }

    /** Extract the role from the cookie (called by the success handler). */
    public String getRole(HttpServletRequest request) {
        AuthRequestWrapper wrapper = getWrapper(request);
        return wrapper != null ? wrapper.role : null;
    }

    /** Extract the client type from the cookie (called by the success handler). */
    public String getClientType(HttpServletRequest request) {
        AuthRequestWrapper wrapper = getWrapper(request);
        return wrapper != null ? wrapper.client : null;
    }

    private OAuth2AuthorizationRequest getCookieValue(HttpServletRequest request) {
        AuthRequestWrapper wrapper = getWrapper(request);
        return wrapper != null ? wrapper.authorizationRequest : null;
    }

    private AuthRequestWrapper getWrapper(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                try {
                    String value = cookie.getValue();
                    int dotIdx = value.lastIndexOf('.');
                    if (dotIdx < 0) return null;

                    String encoded = value.substring(0, dotIdx);
                    String signature = value.substring(dotIdx + 1);

                    if (!sign(encoded).equals(signature)) {
                        return null; // tampered
                    }

                    String json = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
                    return MAPPER.readValue(json, AuthRequestWrapper.class);
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    private void removeCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setAttribute("SameSite", "None");
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
     * Wrapper that bundles the OAuth2 authorization request with role and client type.
     * Uses Jackson polymorphic handling registered via a mixin (see below).
     */
    public static class AuthRequestWrapper {
        public OAuth2AuthorizationRequest authorizationRequest;
        public String role;
        public String client;

        public AuthRequestWrapper() {}

        public AuthRequestWrapper(OAuth2AuthorizationRequest authorizationRequest, String role, String client) {
            this.authorizationRequest = authorizationRequest;
            this.role = role;
            this.client = client;
        }
    }

    static {
        // Register Jackson module/mixin for OAuth2AuthorizationRequest serialization
        MAPPER.findAndRegisterModules();
        MAPPER.addMixIn(OAuth2AuthorizationRequest.class, OAuth2AuthorizationRequestMixin.class);
    }
}
