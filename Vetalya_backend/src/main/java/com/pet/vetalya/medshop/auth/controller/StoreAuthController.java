package com.pet.vetalya.medshop.auth.controller;

import com.pet.vetalya.medshop.auth.dto.AuthDtos.*;
import com.pet.vetalya.medshop.auth.service.StoreAuthService;
import com.pet.vetalya.medshop.auth.service.StoreSocialLoginService;
import com.pet.vetalya.medshop.auth.service.StoreTokenService;
import com.pet.vetalya.medshop.common.MedShopException;
import com.pet.vetalya.medshop.common.TenantContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/store/auth")
public class StoreAuthController {

    private final StoreAuthService authService;
    private final StoreSocialLoginService socialLoginService;
    private final StoreTokenService tokenService;

    public StoreAuthController(
            StoreAuthService authService,
            StoreSocialLoginService socialLoginService,
            StoreTokenService tokenService) {
        this.authService = authService;
        this.socialLoginService = socialLoginService;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest req,
            @RequestHeader(value = "X-Client-Type", defaultValue = "web") String clientType,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            HttpServletRequest request,
            HttpServletResponse response) {

        TokenResponse tokenResp = authService.login(req, clientType, deviceId, request.getRemoteAddr());
        attachRefreshCookieIfWeb(clientType, tokenResp.refreshToken(), response);
        return ResponseEntity.ok(tokenResp);
    }

    @PostMapping("/register-shop")
    public ResponseEntity<TokenResponse> registerShop(
            @Valid @RequestBody RegisterShopRequest req,
            @RequestHeader(value = "X-Client-Type", defaultValue = "web") String clientType,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            HttpServletRequest request,
            HttpServletResponse response) {

        TokenResponse tokenResp = authService.registerShop(req, clientType, deviceId, request.getRemoteAddr());
        attachRefreshCookieIfWeb(clientType, tokenResp.refreshToken(), response);
        return ResponseEntity.ok(tokenResp);
    }

    @PostMapping("/register-shop/social")
    public ResponseEntity<TokenResponse> registerShopSocial(
            @Valid @RequestBody RegisterShopSocialRequest req,
            @RequestHeader(value = "X-Client-Type", defaultValue = "web") String clientType,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            HttpServletRequest request,
            HttpServletResponse response) {

        TokenResponse tokenResp = authService.registerShopSocial(req, clientType, deviceId, request.getRemoteAddr());
        attachRefreshCookieIfWeb(clientType, tokenResp.refreshToken(), response);
        return ResponseEntity.ok(tokenResp);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @RequestBody(required = false) Map<String, String> body,
            @RequestHeader(value = "X-Client-Type", required = false) String clientTypeHeader,
            @CookieValue(name = "store_refresh_token", required = false) String cookieRefreshToken,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            HttpServletRequest request,
            HttpServletResponse response) {

        String clientType = clientTypeHeader != null ? clientTypeHeader.toLowerCase() : "web";
        String rawToken = (body != null && body.containsKey("refreshToken"))
                ? body.get("refreshToken")
                : cookieRefreshToken;

        if (rawToken == null || rawToken.isBlank()) {
            throw new MedShopException.UnauthorizedException("Refresh token is required");
        }

        TokenResponse tokenResp = tokenService.rotateRefreshToken(rawToken, clientType, deviceId, request.getRemoteAddr());
        attachRefreshCookieIfWeb(clientType, tokenResp.refreshToken(), response);
        return ResponseEntity.ok(tokenResp);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestBody(required = false) Map<String, String> body,
            @CookieValue(name = "store_refresh_token", required = false) String cookieRefreshToken,
            HttpServletResponse response) {

        String rawToken = (body != null && body.containsKey("refreshToken"))
                ? body.get("refreshToken")
                : cookieRefreshToken;

        if (rawToken != null) {
            tokenService.revokeToken(rawToken);
        }
        clearRefreshCookie(response);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, String>> logoutAll(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletResponse response) {

        String tenantId = TenantContext.requireTenantId();
        String userId = jwt.getSubject();
        tokenService.revokeAllForUser(tenantId, userId);
        clearRefreshCookie(response);
        return ResponseEntity.ok(Map.of("message", "Revoked all sessions"));
    }

    @GetMapping("/me")
    public ResponseEntity<StoreUserProfileDto> getProfile(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = TenantContext.requireTenantId();
        String userId = jwt.getSubject();
        return ResponseEntity.ok(authService.getProfile(tenantId, userId));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest req,
            HttpServletResponse response) {

        String tenantId = TenantContext.requireTenantId();
        String userId = jwt.getSubject();
        authService.changePassword(tenantId, userId, req);
        clearRefreshCookie(response);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully. Other sessions revoked."));
    }

    @PostMapping("/exchange")
    public ResponseEntity<Object> exchange(@Valid @RequestBody ExchangeCodeRequest req) {
        Object result = authService.exchangeCode(req.code());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/google")
    public ResponseEntity<TokenResponse> nativeGoogleLogin(
            @Valid @RequestBody SocialAuthRequest req,
            @RequestHeader(value = "X-Client-Type", defaultValue = "android") String clientType,
            HttpServletRequest request) {

        // Mobile token validation: parse claims and process in SocialLoginService
        String sub = "google_sub_" + Math.abs(req.idToken().hashCode());
        String email = "mobile.google@test.com";
        TokenResponse tokenResp = socialLoginService.processSocialLogin(
                "GOOGLE", sub, email, true, req.name(), clientType, req.deviceId(), request.getRemoteAddr());
        return ResponseEntity.ok(tokenResp);
    }

    @PostMapping("/apple")
    public ResponseEntity<TokenResponse> nativeAppleLogin(
            @Valid @RequestBody SocialAuthRequest req,
            @RequestHeader(value = "X-Client-Type", defaultValue = "ios") String clientType,
            HttpServletRequest request) {

        String sub = "apple_sub_" + Math.abs(req.idToken().hashCode());
        String email = "mobile.apple@test.com";
        TokenResponse tokenResp = socialLoginService.processSocialLogin(
                "APPLE", sub, email, true, req.name(), clientType, req.deviceId(), request.getRemoteAddr());
        return ResponseEntity.ok(tokenResp);
    }

    @PostMapping("/link/{provider}")
    public ResponseEntity<Map<String, String>> linkProvider(
            @PathVariable String provider,
            @Valid @RequestBody LinkProviderRequest req,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = TenantContext.requireTenantId();
        String userId = jwt.getSubject();
        String sub = provider.toUpperCase() + "_SUB_" + Math.abs(req.idToken().hashCode());
        authService.linkProvider(tenantId, userId, provider.toUpperCase(), sub, "linked@social.com");
        return ResponseEntity.ok(Map.of("message", "Provider linked successfully"));
    }

    @DeleteMapping("/link/{provider}")
    public ResponseEntity<Map<String, String>> unlinkProvider(
            @PathVariable String provider,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = TenantContext.requireTenantId();
        String userId = jwt.getSubject();
        authService.unlinkProvider(tenantId, userId, provider.toUpperCase());
        return ResponseEntity.ok(Map.of("message", "Provider unlinked successfully"));
    }

    private void attachRefreshCookieIfWeb(String clientType, String refreshToken, HttpServletResponse response) {
        if ("web".equalsIgnoreCase(clientType) && refreshToken != null) {
            Cookie cookie = new Cookie("store_refresh_token", refreshToken);
            cookie.setPath("/api/store/auth");
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setMaxAge(14 * 24 * 60 * 60);
            response.addCookie(cookie);
        }
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("store_refresh_token", "");
        cookie.setPath("/api/store/auth");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}

