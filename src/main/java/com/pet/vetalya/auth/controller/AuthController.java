package com.pet.vetalya.auth.controller;

import com.pet.vetalya.auth.apple.AppleTokenRevocationService;
import com.pet.vetalya.auth.service.NativeTokenVerificationService;
import com.pet.vetalya.auth.service.SocialLoginService;
import com.pet.vetalya.auth.service.TokenService;
import com.pet.vetalya.config.AppProperties;
import com.pet.vetalya.dto.CodeExchangeRequest;
import com.pet.vetalya.dto.NativeLoginRequest;
import com.pet.vetalya.dto.RefreshRequest;
import com.pet.vetalya.dto.TokenResponse;
import com.pet.vetalya.entity.AuthProvider;
import com.pet.vetalya.entity.OneTimeCode;
import com.pet.vetalya.entity.UserAuth;
import com.pet.vetalya.repository.AuthProviderRepository;
import com.pet.vetalya.repository.OneTimeCodeRepository;
import com.pet.vetalya.repository.RefreshTokenRepository;
import com.pet.vetalya.repository.UserAuthRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final String REFRESH_COOKIE_NAME = "refresh_token";

    private final TokenService tokenService;
    private final SocialLoginService socialLoginService;
    private final NativeTokenVerificationService nativeTokenVerificationService;
    private final AppleTokenRevocationService appleTokenRevocationService;
    private final OneTimeCodeRepository oneTimeCodeRepo;
    private final UserAuthRepository userAuthRepo;
    private final AuthProviderRepository authProviderRepo;
    private final RefreshTokenRepository refreshTokenRepo;
    private final AppProperties appProperties;

    public AuthController(TokenService tokenService,
                          SocialLoginService socialLoginService,
                          NativeTokenVerificationService nativeTokenVerificationService,
                          AppleTokenRevocationService appleTokenRevocationService,
                          OneTimeCodeRepository oneTimeCodeRepo,
                          UserAuthRepository userAuthRepo,
                          AuthProviderRepository authProviderRepo,
                          RefreshTokenRepository refreshTokenRepo,
                          AppProperties appProperties) {
        this.tokenService = tokenService;
        this.socialLoginService = socialLoginService;
        this.nativeTokenVerificationService = nativeTokenVerificationService;
        this.appleTokenRevocationService = appleTokenRevocationService;
        this.oneTimeCodeRepo = oneTimeCodeRepo;
        this.userAuthRepo = userAuthRepo;
        this.authProviderRepo = authProviderRepo;
        this.refreshTokenRepo = refreshTokenRepo;
        this.appProperties = appProperties;
    }

    /**
     * Refresh access and refresh tokens.
     * Web clients send refresh token in an HttpOnly cookie; mobile clients send in body.
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @RequestBody(required = false) RefreshRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String cookieToken = extractCookie(httpRequest, REFRESH_COOKIE_NAME);
        boolean isWeb = cookieToken != null;
        String rawToken = isWeb ? cookieToken : (request != null ? request.refreshToken() : null);

        if (rawToken == null || rawToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "missing_token", "message", "Refresh token is missing"));
        }

        try {
            String ipAddress = httpRequest.getRemoteAddr();
            TokenService.RotationResult result = tokenService.rotateRefreshToken(rawToken, ipAddress);

            int expiresIn = appProperties.jwt().accessTokenExpiryMinutes() * 60;

            if (isWeb) {
                // Set rotated refresh token back in HttpOnly cookie
                ResponseCookie cookie = buildRefreshCookie(result.refreshToken(), appProperties.jwt().refreshTokenExpiryDays() * 24 * 60 * 60);
                httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
                return ResponseEntity.ok(new TokenResponse(result.accessToken(), null, expiresIn, false, true));
            } else {
                // Mobile client receives both tokens in response body
                return ResponseEntity.ok(new TokenResponse(result.accessToken(), result.refreshToken(), expiresIn, false, true));
            }
        } catch (TokenService.InvalidTokenException e) {
            log.warn("Refresh failed: {}", e.getMessage());
            // Clear cookie on failure
            if (isWeb) {
                httpResponse.addHeader(HttpHeaders.SET_COOKIE, buildRefreshCookie("", 0).toString());
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_token", "message", e.getMessage()));
        }
    }

    /**
     * Exchanges a short-lived one-time authorization code (from mobile redirect flow)
     * for JWT access token and refresh token.
     */
    @PostMapping("/exchange")
    @Transactional
    public ResponseEntity<?> exchange(@RequestBody CodeExchangeRequest request, HttpServletRequest httpRequest) {
        if (request.code() == null || request.code().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_code", "message", "Code is required"));
        }

        String codeHash = TokenService.sha256(request.code());
        OneTimeCode otc = oneTimeCodeRepo.findByCodeHash(codeHash)
                .orElse(null);

        if (otc == null || otc.isUsed() || otc.getExpiresAt().isBefore(Instant.now())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_or_expired_code", "message", "Code is invalid or expired"));
        }

        // Mark code used immediately to prevent replay
        otc.setUsed(true);
        oneTimeCodeRepo.save(otc);

        UserAuth user = userAuthRepo.findById(otc.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found for code"));

        if (!"ACTIVE".equals(user.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "account_inactive", "message", "Account is " + user.getStatus().toLowerCase()));
        }

        String ipAddress = httpRequest.getRemoteAddr();
        String accessToken = tokenService.createAccessToken(user);
        String refreshToken = tokenService.createRefreshToken(user, otc.getClientType(), request.deviceId(), ipAddress);
        int expiresIn = appProperties.jwt().accessTokenExpiryMinutes() * 60;

        return ResponseEntity.ok(new TokenResponse(
                accessToken,
                refreshToken,
                expiresIn,
                otc.isNewUser(),
                otc.isProfileComplete()
        ));
    }

    /**
     * Native Google Sign-In endpoint for mobile apps (Android & iOS).
     * Validates Google id_token cryptographically, links or registers user, issues tokens.
     */
    @PostMapping("/google")
    public ResponseEntity<?> nativeGoogleLogin(@RequestBody NativeLoginRequest request, HttpServletRequest httpRequest) {
        if (request.idToken() == null || request.idToken().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "missing_token", "message", "idToken is required"));
        }

        String role = request.role() != null ? request.role() : "ROLE_PET_OWNER";
        try {
            var userInfo = nativeTokenVerificationService.verifyGoogleToken(request.idToken());
            String ipAddress = httpRequest.getRemoteAddr();

            SocialLoginService.LoginResult result = socialLoginService.processLogin(
                    userInfo, role, "android", request.deviceId(), ipAddress);

            int expiresIn = appProperties.jwt().accessTokenExpiryMinutes() * 60;
            return ResponseEntity.ok(new TokenResponse(
                    result.accessToken(),
                    result.refreshToken(),
                    expiresIn,
                    result.newUser(),
                    result.profileComplete()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "authentication_failed", "message", e.getMessage()));
        } catch (SocialLoginService.AccountBlockedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "account_blocked", "message", e.getMessage()));
        }
    }

    /**
     * Native Apple Sign-In endpoint for mobile apps (iOS).
     * Validates Apple identityToken cryptographically, links or registers user, issues tokens.
     */
    @PostMapping("/apple")
    public ResponseEntity<?> nativeAppleLogin(@RequestBody NativeLoginRequest request, HttpServletRequest httpRequest) {
        if (request.idToken() == null || request.idToken().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "missing_token", "message", "idToken is required"));
        }

        String role = request.role() != null ? request.role() : "ROLE_PET_OWNER";
        try {
            var userInfo = nativeTokenVerificationService.verifyAppleToken(request.idToken(), request.name());
            String ipAddress = httpRequest.getRemoteAddr();

            SocialLoginService.LoginResult result = socialLoginService.processLogin(
                    userInfo, role, "ios", request.deviceId(), ipAddress);

            int expiresIn = appProperties.jwt().accessTokenExpiryMinutes() * 60;
            return ResponseEntity.ok(new TokenResponse(
                    result.accessToken(),
                    result.refreshToken(),
                    expiresIn,
                    result.newUser(),
                    result.profileComplete()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "authentication_failed", "message", e.getMessage()));
        } catch (SocialLoginService.AccountBlockedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "account_blocked", "message", e.getMessage()));
        }
    }

    /**
     * Logout endpoint: revokes refresh token and clears cookie.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestBody(required = false) RefreshRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String cookieToken = extractCookie(httpRequest, REFRESH_COOKIE_NAME);
        String rawToken = cookieToken != null ? cookieToken : (request != null ? request.refreshToken() : null);

        if (rawToken != null && !rawToken.isBlank()) {
            tokenService.revokeRefreshToken(rawToken);
        }

        // Always clear refresh cookie
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, buildRefreshCookie("", 0).toString());

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    /**
     * Account deletion endpoint (Deliverable 8).
     * Revokes Apple token with Apple auth server (if linked), bumps token_version to invalidate
     * all active access tokens, revokes all refresh tokens, marks account DELETED.
     */
    @DeleteMapping("/account")
    @Transactional
    public ResponseEntity<?> deleteAccount(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletResponse httpResponse) {

        String userId = jwt.getSubject();
        UserAuth user = userAuthRepo.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        // If Apple provider linked, revoke Apple refresh token
        List<AuthProvider> providers = authProviderRepo.findAllByUserId(userId);
        for (AuthProvider ap : providers) {
            if ("APPLE".equals(ap.getProvider()) && ap.getProviderRefreshToken() != null) {
                appleTokenRevocationService.revokeToken(ap.getProviderRefreshToken());
            }
        }

        // Invalidate all active tokens immediately
        user.setStatus("DELETED");
        user.setTokenVersion(user.getTokenVersion() + 1);
        userAuthRepo.save(user);

        // Revoke all refresh tokens
        refreshTokenRepo.revokeAllByUserId(userId);

        // Clear web cookie
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, buildRefreshCookie("", 0).toString());

        log.info("User {} deleted account and all active tokens were invalidated", userId);
        return ResponseEntity.ok(Map.of("message", "Account successfully deleted and tokens revoked"));
    }

    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private ResponseCookie buildRefreshCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(true)
                .path("/api/auth")
                .maxAge(maxAgeSeconds)
                .sameSite("Strict")
                .build();
    }
}
