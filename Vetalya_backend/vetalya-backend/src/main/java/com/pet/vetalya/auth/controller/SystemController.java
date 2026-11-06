package com.pet.vetalya.auth.controller;

import com.pet.vetalya.auth.service.SocialLoginService;
import com.pet.vetalya.config.AppProperties;
import com.pet.vetalya.dto.ProviderUserInfo;
import com.pet.vetalya.dto.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Public health, diagnostic, and development login endpoints.
 */
@RestController
public class SystemController {

    private static final Logger log = LoggerFactory.getLogger(SystemController.class);

    private final SocialLoginService socialLoginService;
    private final AppProperties appProperties;
    private final JdbcTemplate jdbcTemplate;

    public SystemController(SocialLoginService socialLoginService, AppProperties appProperties, JdbcTemplate jdbcTemplate) {
        this.socialLoginService = socialLoginService;
        this.appProperties = appProperties;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        return ResponseEntity.ok(Map.of(
                "app", "Vetalya API",
                "status", "RUNNING",
                "version", "0.0.1-SNAPSHOT",
                "timestamp", Instant.now().toString(),
                "endpoints", Map.of(
                        "googleLogin", "/oauth2/authorization/google?role=ROLE_PET_OWNER&client=web",
                        "appleLogin", "/oauth2/authorization/apple?role=ROLE_PET_OWNER&client=web",
                        "devLogin", "/api/public/dev-login?role=ROLE_PET_OWNER",
                        "logs", "/api/public/logs",
                        "refresh", "/api/auth/refresh",
                        "exchange", "/api/auth/exchange"
                )
        ));
    }

    @GetMapping("/api/public/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "database", "CONNECTED"
        ));
    }

    /**
     * Inspect all database tables and recent records in Supabase PostgreSQL.
     */
    @GetMapping("/api/public/db-inspect")
    public ResponseEntity<?> dbInspect() {
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "CONNECTED");
            result.put("user_auth", jdbcTemplate.queryForList(
                    "SELECT user_id, role, email, status, token_version, last_login_at, created_at FROM user_auth ORDER BY created_at DESC LIMIT 10"));
            result.put("auth_provider", jdbcTemplate.queryForList(
                    "SELECT id, user_id, provider, provider_user_id, provider_email, created_at FROM auth_provider ORDER BY id DESC LIMIT 10"));
            result.put("refresh_token", jdbcTemplate.queryForList(
                    "SELECT id, user_id, client_type, device_id, ip_address, expires_at, used_at, revoked, created_at FROM refresh_token ORDER BY id DESC LIMIT 10"));
            result.put("one_time_code", jdbcTemplate.queryForList(
                    "SELECT id, user_id, client_type, new_user, profile_complete, used, expires_at, created_at FROM one_time_code ORDER BY id DESC LIMIT 10"));
            result.put("pet_owner", jdbcTemplate.queryForList(
                    "SELECT * FROM pet_owner ORDER BY created_at DESC LIMIT 10"));
            result.put("vet", jdbcTemplate.queryForList(
                    "SELECT * FROM vet ORDER BY created_at DESC LIMIT 10"));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to inspect database", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Development / Testing Login:
     * Generates a real RSA-signed JWT and Supabase database-backed session without requiring
     * external Google or Apple Cloud Developer account setup.
     */
    @PostMapping("/api/public/dev-login")
    public ResponseEntity<?> devLogin(
            @RequestParam(defaultValue = "ROLE_PET_OWNER") String role,
            @RequestParam(defaultValue = "dev-tester@vetalya.local") String email,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        log.info("Development login requested for role: {} email: {}", role, email);
        String ipAddress = httpRequest.getRemoteAddr();
        String safeSub = "dev-" + email.replaceAll("[^a-zA-Z0-9]", "-");

        ProviderUserInfo info = new ProviderUserInfo(
                "GOOGLE",
                safeSub,
                email,
                true,
                "Dev",
                "User",
                null
        );

        try {
            SocialLoginService.LoginResult result = socialLoginService.processLogin(
                    info, role, "web", null, ipAddress);

            // Issue refresh cookie with local-friendly lax attributes
            ResponseCookie cookie = ResponseCookie.from("refresh_token", result.refreshToken())
                    .httpOnly(true)
                    .secure(false) // allows HTTP localhost testing
                    .path("/")
                    .maxAge(appProperties.jwt().refreshTokenExpiryDays() * 24L * 60 * 60)
                    .sameSite("Lax")
                    .build();
            httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            int expiresIn = appProperties.jwt().accessTokenExpiryMinutes() * 60;
            return ResponseEntity.ok(new TokenResponse(
                    result.accessToken(),
                    result.refreshToken(),
                    expiresIn,
                    result.newUser(),
                    result.profileComplete()
            ));
        } catch (SocialLoginService.AccountBlockedException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "account_blocked", "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "invalid_role", "message", e.getMessage()));
        }
    }

    /**
     * Endpoint to view the latest backend server logs.
     */
    @GetMapping("/api/public/logs")
    public ResponseEntity<?> getLogs() {
        try {
            Path logPath = Paths.get("target/spring-boot.log");
            if (!Files.exists(logPath)) {
                return ResponseEntity.ok(Map.of(
                        "status", "info",
                        "lines", List.of("// Server log file not initialized yet or writing to console.")
                ));
            }
            List<String> allLines = Files.readAllLines(logPath);
            int start = Math.max(0, allLines.size() - 80);
            List<String> recentLines = allLines.subList(start, allLines.size());
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "totalLines", allLines.size(),
                    "lines", recentLines
            ));
        } catch (Exception e) {
            log.error("Failed to read server logs", e);
            return ResponseEntity.ok(Map.of(
                    "status", "error",
                    "lines", List.of("Error reading logs: " + e.getMessage())
            ));
        }
    }
}
