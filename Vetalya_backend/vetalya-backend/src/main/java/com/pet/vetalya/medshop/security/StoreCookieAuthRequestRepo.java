package com.pet.vetalya.medshop.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Cookie-based AuthorizationRequestRepository for stateless OAuth2 login.
 * Encodes OAuth2AuthorizationRequest and preserves ?client=web|android|ios
 * parameter.
 * Uses SameSite=None; Secure for Apple cross-site form_post compatibility.
 */
@Component
public class StoreCookieAuthRequestRepo implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String OAUTH2_AUTH_REQUEST_COOKIE_NAME = "store_oauth2_auth_request";
    public static final String CLIENT_TYPE_COOKIE_NAME = "store_oauth2_client_type";
    private static final int COOKIE_EXPIRE_SECONDS = 180;

    private final byte[] hmacKey;

    public StoreCookieAuthRequestRepo() {
        this.hmacKey = "medshop-stateless-cookie-signing-secret-key-2026".getBytes(StandardCharsets.UTF_8);
    }

    public StoreCookieAuthRequestRepo(String secret) {
        this.hmacKey = (secret != null ? secret : "medshop-stateless-cookie-signing-secret-key-2026")
                .getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        Cookie cookie = getCookie(request, OAUTH2_AUTH_REQUEST_COOKIE_NAME);
        if (cookie == null || cookie.getValue() == null) {
            return null;
        }
        return deserialize(cookie.getValue());
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (authorizationRequest == null) {
            deleteCookie(response, OAUTH2_AUTH_REQUEST_COOKIE_NAME);
            deleteCookie(response, CLIENT_TYPE_COOKIE_NAME);
            return;
        }

        String serialized = serialize(authorizationRequest);
        addSameSiteNoneCookie(response, OAUTH2_AUTH_REQUEST_COOKIE_NAME, serialized, COOKIE_EXPIRE_SECONDS);

        String clientType = request.getParameter("client");
        if (clientType != null && !clientType.isBlank()) {
            addSameSiteNoneCookie(response, CLIENT_TYPE_COOKIE_NAME, clientType.trim().toLowerCase(),
                    COOKIE_EXPIRE_SECONDS);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
            HttpServletResponse response) {
        OAuth2AuthorizationRequest authReq = loadAuthorizationRequest(request);
        deleteCookie(response, OAUTH2_AUTH_REQUEST_COOKIE_NAME);
        // Note: Do not delete CLIENT_TYPE_COOKIE_NAME immediately here so
        // success/failure handlers can read it
        return authReq;
    }

    public String getAndClearClientType(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = getCookie(request, CLIENT_TYPE_COOKIE_NAME);
        deleteCookie(response, CLIENT_TYPE_COOKIE_NAME);
        return cookie != null ? cookie.getValue() : "web";
    }

    private Cookie getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (c.getName().equals(name)) {
                    return c;
                }
            }
        }
        return null;
    }

    private void addSameSiteNoneCookie(HttpServletResponse response, String name, String value, int maxAge) {
        // SameSite=None requires Secure. Header format guarantees compliance for Apple
        // form_post
        String headerValue = String.format("%s=%s; Path=/api/store; Max-Age=%d; HttpOnly; Secure; SameSite=None",
                name, value, maxAge);
        response.addHeader("Set-Cookie", headerValue);
    }

    private void deleteCookie(HttpServletResponse response, String name) {
        String headerValue = String.format("%s=; Path=/api/store; Max-Age=0; HttpOnly; Secure; SameSite=None", name);
        response.addHeader("Set-Cookie", headerValue);
    }

    private String serialize(OAuth2AuthorizationRequest request) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(request);
            }
            byte[] bytes = baos.toByteArray();
            String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            String sig = sign(payload);
            return payload + "." + sig;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize OAuth2 request", e);
        }
    }

    private OAuth2AuthorizationRequest deserialize(String value) {
        try {
            int dot = value.lastIndexOf('.');
            if (dot <= 0)
                return null;
            String payload = value.substring(0, dot);
            String sig = value.substring(dot + 1);
            if (!MessageDigest.isEqual(sign(payload).getBytes(StandardCharsets.UTF_8),
                    sig.getBytes(StandardCharsets.UTF_8))) {
                return null;
            }
            byte[] bytes = Base64.getUrlDecoder().decode(payload);
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
                return (OAuth2AuthorizationRequest) ois.readObject();
            }
        } catch (Exception e) {
            return null;
        }
    }

    private String sign(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
    }
}
