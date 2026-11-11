package com.pet.vetalya.auth.controller;

import com.pet.vetalya.auth.apple.AppleTokenRevocationService;
import com.pet.vetalya.auth.service.NativeTokenVerificationService;
import com.pet.vetalya.auth.service.OtpService;
import com.pet.vetalya.auth.service.SocialLoginService;
import com.pet.vetalya.auth.service.TokenService;
import com.pet.vetalya.config.AppProperties;
import com.pet.vetalya.dto.CodeExchangeRequest;
import com.pet.vetalya.dto.NativeLoginRequest;
import com.pet.vetalya.dto.ProviderUserInfo;
import com.pet.vetalya.dto.RefreshRequest;
import com.pet.vetalya.dto.TokenResponse;
import com.pet.vetalya.entity.AuthProvider;
import com.pet.vetalya.entity.OneTimeCode;
import com.pet.vetalya.entity.PetOwner;
import com.pet.vetalya.entity.UserAuth;
import com.pet.vetalya.entity.Vet;
import com.pet.vetalya.repository.AuthProviderRepository;
import com.pet.vetalya.repository.OneTimeCodeRepository;
import com.pet.vetalya.repository.PetOwnerRepository;
import com.pet.vetalya.repository.RefreshTokenRepository;
import com.pet.vetalya.repository.UserAuthRepository;
import com.pet.vetalya.repository.VetRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
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
    private final PasswordEncoder passwordEncoder;
    private final PetOwnerRepository petOwnerRepo;
    private final VetRepository vetRepo;
    private final OtpService otpService;

    public AuthController(TokenService tokenService,
                          SocialLoginService socialLoginService,
                          NativeTokenVerificationService nativeTokenVerificationService,
                          AppleTokenRevocationService appleTokenRevocationService,
                          OneTimeCodeRepository oneTimeCodeRepo,
                          UserAuthRepository userAuthRepo,
                          AuthProviderRepository authProviderRepo,
                          RefreshTokenRepository refreshTokenRepo,
                          AppProperties appProperties,
                          PasswordEncoder passwordEncoder,
                          PetOwnerRepository petOwnerRepo,
                          VetRepository vetRepo,
                          OtpService otpService) {
        this.tokenService = tokenService;
        this.socialLoginService = socialLoginService;
        this.nativeTokenVerificationService = nativeTokenVerificationService;
        this.appleTokenRevocationService = appleTokenRevocationService;
        this.oneTimeCodeRepo = oneTimeCodeRepo;
        this.userAuthRepo = userAuthRepo;
        this.authProviderRepo = authProviderRepo;
        this.refreshTokenRepo = refreshTokenRepo;
        this.appProperties = appProperties;
        this.passwordEncoder = passwordEncoder;
        this.petOwnerRepo = petOwnerRepo;
        this.vetRepo = vetRepo;
        this.otpService = otpService;
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
                ResponseCookie cookie = buildRefreshCookie(result.refreshToken(), appProperties.jwt().refreshTokenExpiryDays() * 24L * 60 * 60, httpRequest.isSecure());
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
                httpResponse.addHeader(HttpHeaders.SET_COOKIE, buildRefreshCookie("", 0, httpRequest.isSecure()).toString());
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
    public ResponseEntity<?> exchange(
            @RequestBody CodeExchangeRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (request == null || request.code() == null || request.code().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_code", "message", "Code is required"));
        }

        String codeHash = TokenService.sha256(request.code());
        OneTimeCode otc = oneTimeCodeRepo.findByCodeHash(codeHash)
                .orElse(null);

        if (otc == null || otc.isUsed() || otc.getExpiresAt() == null || otc.getExpiresAt().isBefore(Instant.now())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_or_expired_code", "message", "Code is invalid or expired"));
        }

        // Mark code used immediately to prevent replay
        otc.setUsed(true);
        oneTimeCodeRepo.save(otc);

        if (otc.getUserId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_code", "message", "Code has no associated user"));
        }

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

        // Set refresh token cookie on the active domain/proxy path
        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(httpRequest.isSecure())
                .path("/")
                .maxAge(appProperties.jwt().refreshTokenExpiryDays() * 24L * 60 * 60)
                .sameSite(httpRequest.isSecure() ? "None" : "Lax")
                .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

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
    public ResponseEntity<?> nativeGoogleLogin(
            @RequestBody NativeLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (request == null || request.idToken() == null || request.idToken().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "missing_token", "message", "idToken is required"));
        }

        String role = request.role() != null ? request.role() : "ROLE_PET_OWNER";
        if ("PET_PARENT".equalsIgnoreCase(role)) role = "ROLE_PET_OWNER";
        if ("VETERINARIAN".equalsIgnoreCase(role)) role = "ROLE_VET";

        try {
            var userInfo = nativeTokenVerificationService.verifyGoogleToken(request.idToken());
            String ipAddress = httpRequest.getRemoteAddr();

            SocialLoginService.LoginResult result = socialLoginService.processLogin(
                    userInfo, role, "web", request.deviceId(), ipAddress);

            ResponseCookie cookie = buildRefreshCookie(result.refreshToken(),
                    appProperties.jwt().refreshTokenExpiryDays() * 24L * 60 * 60, httpRequest.isSecure());
            httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            int expiresIn = appProperties.jwt().accessTokenExpiryMinutes() * 60;
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("token", result.accessToken());
            response.put("accessToken", result.accessToken());
            if (result.refreshToken() != null) {
                response.put("refreshToken", result.refreshToken());
            }
            response.put("expiresIn", expiresIn);
            response.put("newUser", result.newUser());
            response.put("profileComplete", result.profileComplete());
            response.put("user", getUserMap(result.user()));

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Invalid Google ID token";
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "authentication_failed", "message", msg));
        } catch (SocialLoginService.AccountBlockedException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Account is blocked";
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "account_blocked", "message", msg));
        }
    }

    /**
     * Native Apple Sign-In endpoint for mobile apps (iOS).
     * Validates Apple identityToken cryptographically, links or registers user, issues tokens.
     */
    @PostMapping("/apple")
    public ResponseEntity<?> nativeAppleLogin(@RequestBody NativeLoginRequest request, HttpServletRequest httpRequest) {
        if (request == null || request.idToken() == null || request.idToken().isBlank()) {
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
            String msg = e.getMessage() != null ? e.getMessage() : "Invalid Apple ID token";
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "authentication_failed", "message", msg));
        } catch (SocialLoginService.AccountBlockedException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Account is blocked";
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "account_blocked", "message", msg));
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

        // Clear refresh cookie for both "/" and legacy "/api/auth" paths
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, buildRefreshCookie("", 0, httpRequest.isSecure()).toString());
        ResponseCookie legacyCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(httpRequest.isSecure())
                .path("/api/auth")
                .maxAge(0)
                .sameSite(httpRequest.isSecure() ? "None" : "Lax")
                .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, legacyCookie.toString());

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
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (jwt == null || jwt.getSubject() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }

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
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, buildRefreshCookie("", 0, httpRequest.isSecure()).toString());

        log.info("User {} deleted account and all active tokens were invalidated", userId);
        return ResponseEntity.ok(Map.of("message", "Account successfully deleted and tokens revoked"));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Vetalya Authentication Service",
                "authMethods", List.of("JWT", "Google OAuth ID Token", "Local Password")
        ));
    }

    @PostMapping("/demo-login")
    public ResponseEntity<?> demoLogin(
            @RequestBody(required = false) Map<String, String> request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String roleParam = (request != null && request.get("role") != null) ? request.get("role") : "PET_PARENT";
        String normalizedRole;
        String email;
        String name;
        switch (roleParam.toUpperCase()) {
            case "VETERINARIAN", "ROLE_VET" -> {
                normalizedRole = "ROLE_VET";
                email = "dr.priya@koramangalavet.in";
                name = "Dr. Priya Sharma";
            }
            case "CLINIC_STAFF", "ROLE_CLINIC_STAFF" -> {
                normalizedRole = "ROLE_CLINIC_STAFF";
                email = "reception@koramangalavet.in";
                name = "Ananya Deshmukh";
            }
            case "CLINIC_ADMIN", "ROLE_CLINIC_ADMIN" -> {
                normalizedRole = "ROLE_CLINIC_ADMIN";
                email = "director@koramangalavet.in";
                name = "Dr. Rajesh Kulkarni";
            }
            case "PLATFORM_ADMIN", "ROLE_PLATFORM_ADMIN" -> {
                normalizedRole = "ROLE_PLATFORM_ADMIN";
                email = "regulatory@vetalya.in";
                name = "Vetalya Compliance Officer";
            }
            default -> {
                normalizedRole = "ROLE_PET_OWNER";
                email = "rahul.sharma@example.com";
                name = "Rahul Sharma";
            }
        }

        String ipAddress = httpRequest.getRemoteAddr();
        String safeSub = "demo-" + normalizedRole.toLowerCase();

        ProviderUserInfo info = new ProviderUserInfo(
                "GOOGLE",
                safeSub,
                email,
                true,
                name.contains(" ") ? name.substring(0, name.indexOf(' ')) : name,
                name.contains(" ") ? name.substring(name.indexOf(' ') + 1) : "",
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80"
        );

        try {
            SocialLoginService.LoginResult result = socialLoginService.processLogin(
                    info, normalizedRole, "web", null, ipAddress);

            ResponseCookie cookie = buildRefreshCookie(result.refreshToken(),
                    appProperties.jwt().refreshTokenExpiryDays() * 24L * 60 * 60, httpRequest.isSecure());
            httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            Map<String, Object> userMap = new LinkedHashMap<>();
            userMap.put("id", result.user().getUserId());
            userMap.put("name", name);
            userMap.put("fullName", name);
            userMap.put("email", email);
            userMap.put("role", normalizedRole);
            userMap.put("authProvider", "LOCAL / DEMO");
            userMap.put("avatar", info.pictureUrl());

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("token", result.accessToken());
            resp.put("accessToken", result.accessToken());
            if (result.refreshToken() != null) {
                resp.put("refreshToken", result.refreshToken());
            }
            resp.put("user", userMap);

            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("Demo login error", e);
            String msg = e.getMessage() != null ? e.getMessage() : "Demo login failed";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "demo_login_failed", "message", msg));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody(required = false) Map<String, Object> request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Request body is required"));
        }
        String email = (String) request.get("email");
        String password = (String) request.get("password");

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email and password are required"));
        }

        email = email.trim().toLowerCase();
        UserAuth user = userAuthRepo.findByEmail(email).orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or password"));
        }

        if ("BLOCKED".equals(user.getStatus()) || "DELETED".equals(user.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Account is " + user.getStatus().toLowerCase()));
        }

        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or password. If you registered via Google, please use Google sign-in."));
        }

        user.setLastLoginAt(Instant.now());
        userAuthRepo.save(user);

        String ipAddress = httpRequest.getRemoteAddr();
        String accessToken = tokenService.createAccessToken(user);
        String refreshToken = tokenService.createRefreshToken(user, "web", null, ipAddress);

        ResponseCookie cookie = buildRefreshCookie(refreshToken,
                appProperties.jwt().refreshTokenExpiryDays() * 24L * 60 * 60, httpRequest.isSecure());
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        Map<String, Object> userMap = getUserMap(user);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("token", accessToken);
        resp.put("accessToken", accessToken);
        if (refreshToken != null) {
            resp.put("refreshToken", refreshToken);
        }
        resp.put("user", userMap);

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody(required = false) Map<String, Object> request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Request body is required"));
        }
        String fullName = (String) request.get("fullName");
        if (fullName == null || fullName.isBlank()) {
            fullName = (String) request.get("name");
        }
        String email = (String) request.get("email");
        String password = (String) request.get("password");
        String phone = (String) request.get("phone");
        String roleParam = (String) request.get("role");

        if (fullName == null || fullName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Full name is required"));
        }
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email and password are required"));
        }
        if (phone == null || phone.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mobile phone number is required"));
        }

        email = email.trim().toLowerCase();
        phone = phone.trim();

        if (userAuthRepo.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "An account with this email address already exists"));
        }

        if (userAuthRepo.findFirstByPhone(phone).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "An account with this mobile phone number already exists"));
        }

        // OTP verification for both Email and Mobile Phone
        String emailOtp = (String) request.get("emailOtp");
        if (emailOtp == null) emailOtp = (String) request.get("emailCode");
        String phoneOtp = (String) request.get("phoneOtp");
        if (phoneOtp == null) phoneOtp = (String) request.get("phoneCode");
        String singleOtp = (String) request.get("otp");

        if (emailOtp == null && singleOtp != null && !singleOtp.isBlank()) {
            emailOtp = singleOtp;
        }
        if (phoneOtp == null && singleOtp != null && !singleOtp.isBlank()) {
            phoneOtp = singleOtp;
        }

        // 1. Verify Email OTP
        boolean emailVerified = false;
        if (emailOtp != null && !emailOtp.isBlank()) {
            emailVerified = otpService.verifyOtp(email, emailOtp, "REGISTRATION")
                         || otpService.verifyOtp(email, emailOtp, "VERIFICATION");
        }
        if (!emailVerified) {
            emailVerified = otpService.isTargetVerifiedRecently(email, "REGISTRATION")
                         || otpService.isTargetVerifiedRecently(email, "VERIFICATION");
        }
        if (!emailVerified) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "email_verification_failed",
                    "message", "Email verification required. Please verify the 6-digit OTP sent to " + email
            ));
        }

        // 2. Verify Phone OTP
        boolean phoneVerified = false;
        if (phoneOtp != null && !phoneOtp.isBlank()) {
            phoneVerified = otpService.verifyOtp(phone, phoneOtp, "REGISTRATION")
                         || otpService.verifyOtp(phone, phoneOtp, "VERIFICATION");
        }
        if (!phoneVerified) {
            phoneVerified = otpService.isTargetVerifiedRecently(phone, "REGISTRATION")
                         || otpService.isTargetVerifiedRecently(phone, "VERIFICATION");
        }
        if (!phoneVerified) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "phone_verification_failed",
                    "message", "Mobile number verification required. Please verify the 6-digit OTP sent to " + phone
            ));
        }

        String role = (roleParam != null && (roleParam.equalsIgnoreCase("VETERINARIAN") || roleParam.equalsIgnoreCase("ROLE_VET")))
                ? "ROLE_VET" : "ROLE_PET_OWNER";

        String trimmedName = fullName.trim();
        int spaceIdx = trimmedName.indexOf(' ');
        String firstName = spaceIdx > 0 ? trimmedName.substring(0, spaceIdx) : trimmedName;
        String lastName = spaceIdx > 0 ? trimmedName.substring(spaceIdx + 1).trim() : "";

        ProviderUserInfo info = new ProviderUserInfo("GOOGLE", "reg-" + email, email, true, firstName, lastName, null);
        SocialLoginService.LoginResult lr = socialLoginService.processLogin(info, role, "web", null, httpRequest.getRemoteAddr());

        String councilReg = (String) request.get("councilRegNumber");
        if (councilReg == null) councilReg = (String) request.get("registrationNo");

        UserAuth user = lr.user();
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setPhone(phone);
        user.setEmailVerified(true);
        user.setPhoneVerified(true);
        userAuthRepo.save(user);

        if ("ROLE_VET".equals(role)) {
            final String regNumber = councilReg;
            vetRepo.findById(user.getUserId()).ifPresent(v -> {
                if (v != null) {
                    if (regNumber != null && !regNumber.isBlank()) {
                        v.setRegistrationNo(regNumber);
                    }
                    v.setFirstName(firstName);
                    v.setLastName(lastName);
                    v.setVerified(true);
                    vetRepo.save(v);
                }
            });
        } else {
            petOwnerRepo.findById(user.getUserId()).ifPresent(po -> {
                if (po != null) {
                    po.setFirstName(firstName);
                    po.setLastName(lastName);
                    petOwnerRepo.save(po);
                }
            });
        }

        ResponseCookie cookie = buildRefreshCookie(lr.refreshToken(),
                appProperties.jwt().refreshTokenExpiryDays() * 24L * 60 * 60, httpRequest.isSecure());
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        Map<String, Object> userMap = getUserMap(user);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("token", lr.accessToken());
        resp.put("accessToken", lr.accessToken());
        if (lr.refreshToken() != null) {
            resp.put("refreshToken", lr.refreshToken());
        }
        resp.put("user", userMap);

        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    /**
     * Dispatch a 6-digit OTP to mobile phone number or email address.
     */
    @PostMapping("/otp/send")
    public ResponseEntity<?> sendOtp(@RequestBody(required = false) Map<String, String> request) {
        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Request body is required"));
        }
        String phone = request.get("phone");
        String email = request.get("email");
        String target = request.get("target");
        if (target == null || target.isBlank()) {
            target = (phone != null && !phone.isBlank()) ? phone.trim() : (email != null ? email.trim() : null);
        }
        String purpose = request.get("purpose");

        if (target == null || target.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mobile number or email target is required to send OTP"));
        }

        try {
            OtpService.OtpResult result = otpService.sendOtp(target, purpose);
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("message", result.message());
            resp.put("target", target);
            resp.put("expiresInSeconds", result.expiresInSeconds());
            resp.put("devOtp", result.otpCode()); // mock SMS delivery for instant testing
            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "Failed to dispatch OTP";
            return ResponseEntity.badRequest().body(Map.of("message", msg));
        }
    }

    /**
     * Verify the 6-digit OTP entered by the user.
     */
    @PostMapping("/otp/verify")
    public ResponseEntity<?> verifyOtp(@RequestBody(required = false) Map<String, String> request) {
        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Request body is required"));
        }
        String phone = request.get("phone");
        String email = request.get("email");
        String target = request.get("target");
        if (target == null || target.isBlank()) {
            target = (phone != null && !phone.isBlank()) ? phone.trim() : (email != null ? email.trim() : null);
        }
        String otp = request.get("otp");
        if (otp == null) otp = request.get("code");
        String purpose = request.get("purpose");

        if (target == null || otp == null || otp.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Target and 6-digit OTP code are required"));
        }

        boolean valid = otpService.verifyOtp(target, otp, purpose);
        if (valid) {
            return ResponseEntity.ok(Map.of(
                    "verified", true,
                    "message", "Verification code confirmed successfully",
                    "target", target
            ));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "verified", false,
                    "message", "Invalid or expired OTP code. Please check and try again."
            ));
        }
    }

    /**
     * Passwordless Login using Mobile Number + 6-digit OTP.
     */
    @PostMapping("/login-otp")
    public ResponseEntity<?> loginWithOtp(
            @RequestBody(required = false) Map<String, String> request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Request body is required"));
        }
        String phone = request.get("phone");
        String otp = request.get("otp");
        if (otp == null) otp = request.get("code");

        if (phone == null || phone.isBlank() || otp == null || otp.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mobile number and 6-digit OTP are required"));
        }

        phone = phone.trim();
        boolean valid = otpService.verifyOtp(phone, otp, "LOGIN");
        if (!valid) {
            valid = otpService.verifyOtp(phone, otp, "VERIFICATION");
        }

        if (!valid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid or expired OTP code"));
        }

        UserAuth user = userAuthRepo.findFirstByPhone(phone).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "message", "No account registered with phone " + phone + ". Please create an account first."
            ));
        }

        if ("BLOCKED".equals(user.getStatus()) || "DELETED".equals(user.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Account is " + user.getStatus().toLowerCase()));
        }

        user.setPhoneVerified(true);
        user.setLastLoginAt(Instant.now());
        userAuthRepo.save(user);

        String ipAddress = httpRequest.getRemoteAddr();
        String accessToken = tokenService.createAccessToken(user);
        String refreshToken = tokenService.createRefreshToken(user, "web", null, ipAddress);

        ResponseCookie cookie = buildRefreshCookie(refreshToken,
                appProperties.jwt().refreshTokenExpiryDays() * 24L * 60 * 60, httpRequest.isSecure());
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        Map<String, Object> userMap = getUserMap(user);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("token", accessToken);
        resp.put("accessToken", accessToken);
        if (refreshToken != null) {
            resp.put("refreshToken", refreshToken);
        }
        resp.put("user", userMap);

        return ResponseEntity.ok(resp);
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }
        UserAuth user = userAuthRepo.findById(jwt.getSubject()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
        }
        return ResponseEntity.ok(getUserMap(user));
    }

    private Map<String, Object> getUserMap(UserAuth user) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (user == null) {
            return map;
        }
        map.put("id", user.getUserId());
        map.put("email", user.getEmail());
        map.put("role", user.getRole());
        map.put("status", user.getStatus());
        map.put("phone", user.getPhone());
        map.put("emailVerified", user.isEmailVerified());
        map.put("phoneVerified", user.isPhoneVerified());
        map.put("authProvider", "LOCAL");

        if ("ROLE_VET".equals(user.getRole())) {
            vetRepo.findById(user.getUserId()).ifPresent(v -> {
                if (v != null) {
                    map.put("firstName", v.getFirstName());
                    map.put("lastName", v.getLastName());
                    map.put("registrationNo", v.getRegistrationNo());
                    map.put("councilRegNumber", v.getRegistrationNo());
                    String fn = v.getFirstName() != null ? v.getFirstName() : "";
                    String ln = v.getLastName() != null ? v.getLastName() : "";
                    map.put("fullName", (fn + " " + ln).trim());
                    map.put("avatarUrl", v.getPhotoUrl());
                }
            });
        } else {
            petOwnerRepo.findById(user.getUserId()).ifPresent(po -> {
                if (po != null) {
                    map.put("firstName", po.getFirstName());
                    map.put("lastName", po.getLastName());
                    String fn = po.getFirstName() != null ? po.getFirstName() : "";
                    String ln = po.getLastName() != null ? po.getLastName() : "";
                    map.put("fullName", (fn + " " + ln).trim());
                }
            });
        }
        Object fnObj = map.get("fullName");
        if (fnObj == null || fnObj.toString().isBlank()) {
            map.put("fullName", (user.getEmail() != null && !user.getEmail().isBlank())
                    ? user.getEmail().split("@")[0] : "Vetalya User");
        }
        map.put("name", map.get("fullName"));
        return map;
    }

    private String extractCookie(HttpServletRequest request, String name) {
        if (request == null || request.getCookies() == null || name == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> c != null && name.equals(c.getName()))
                .map(c -> c.getValue())
                .findFirst()
                .orElse(null);
    }

    private ResponseCookie buildRefreshCookie(String value, long maxAgeSeconds, boolean isSecure) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(isSecure)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite(isSecure ? "None" : "Lax")
                .build();
    }
}
