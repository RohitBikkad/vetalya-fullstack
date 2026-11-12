package com.pet.vetalya.medshop.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.vetalya.medshop.auth.dto.AuthDtos.TokenResponse;
import com.pet.vetalya.medshop.auth.entity.StoreRefreshToken;
import com.pet.vetalya.medshop.auth.repository.StoreRefreshTokenRepository;
import com.pet.vetalya.medshop.common.MedShopException;
import com.pet.vetalya.medshop.security.StoreJwtConfig;
import com.pet.vetalya.medshop.user.entity.StoreUser;
import com.pet.vetalya.medshop.user.repository.StoreUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class StoreTokenService {

    private static final Logger log = LoggerFactory.getLogger(StoreTokenService.class);

    private final JwtEncoder storeJwtEncoder;
    private final StoreRefreshTokenRepository refreshTokenRepo;
    private final StoreUserRepository userRepo;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();
    private final byte[] onboardingSigningKey;

    @Value("${app.store.jwt.access-token-expiry-minutes:15}")
    private long accessTokenExpiryMinutes;

    @Value("${app.store.jwt.refresh-token-expiry-days:14}")
    private long refreshTokenExpiryDays;

    public StoreTokenService(
            @Qualifier("storeJwtEncoder") JwtEncoder storeJwtEncoder,
            StoreRefreshTokenRepository refreshTokenRepo,
            StoreUserRepository userRepo,
            ObjectMapper objectMapper,
            @Value("${app.store.jwt.onboarding-secret:medshop-onboarding-secret-key-2026}") String onboardingSecret) {
        this.storeJwtEncoder = storeJwtEncoder;
        this.refreshTokenRepo = refreshTokenRepo;
        this.userRepo = userRepo;
        this.objectMapper = objectMapper;
        this.onboardingSigningKey = onboardingSecret.getBytes(StandardCharsets.UTF_8);
    }

    public String createAccessToken(StoreUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(accessTokenExpiryMinutes, ChronoUnit.MINUTES);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(StoreJwtConfig.STORE_ISSUER)
                .audience(List.of(StoreJwtConfig.STORE_AUDIENCE))
                .subject(user.getId())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("tenant_id", user.getTenantId())
                .claim("roles", List.of("ROLE_" + user.getRole()))
                .claim("ver", user.getTokenVersion())
                .build();

        JwsHeader header = JwsHeader.with(() -> "RS256").keyId("medshop-jwt-key").build();
        return storeJwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @Transactional
    public String createRefreshToken(StoreUser user, UUID familyId, String clientType, String deviceId, String ipAddress) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String hash = sha256(rawToken);

        Instant expiresAt = Instant.now().plus(refreshTokenExpiryDays, ChronoUnit.DAYS);
        UUID actualFamily = familyId != null ? familyId : UUID.randomUUID();

        StoreRefreshToken token = new StoreRefreshToken(
                user.getTenantId(), user.getId(), hash, actualFamily, clientType, deviceId, ipAddress, expiresAt);
        refreshTokenRepo.save(token);

        return rawToken;
    }

    @Transactional
    public TokenResponse rotateRefreshToken(String rawRefreshToken, String clientType, String deviceId, String ipAddress) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new MedShopException.UnauthorizedException("Refresh token is missing");
        }

        String hash = sha256(rawRefreshToken);
        StoreRefreshToken token = refreshTokenRepo.findByTokenHash(hash)
                .orElseThrow(() -> new MedShopException.UnauthorizedException("Invalid refresh token"));

        if (token.isRevoked() || token.getUsedAt() != null) {
            // Token reuse detected! Revoke the whole family
            log.warn("Refresh token reuse detected for family {}! Revoking family.", token.getFamilyId());
            refreshTokenRepo.revokeFamily(token.getFamilyId());
            throw new MedShopException.UnauthorizedException("Refresh token reuse detected. Session terminated.");
        }

        if (token.getExpiresAt().isBefore(Instant.now())) {
            token.setRevoked(true);
            refreshTokenRepo.save(token);
            throw new MedShopException.UnauthorizedException("Refresh token expired");
        }

        // Mark current token as used
        token.setUsedAt(Instant.now());
        refreshTokenRepo.save(token);

        // Load user
        StoreUser user = userRepo.findByTenantIdAndId(token.getTenantId(), token.getUserId())
                .orElseThrow(() -> new MedShopException.UnauthorizedException("User not found"));

        if (!user.isActive()) {
            throw new MedShopException.UnauthorizedException("User is deactivated");
        }

        // Issue new access token and rotate refresh token with same familyId
        String newAccessToken = createAccessToken(user);
        String newRefreshToken = createRefreshToken(user, token.getFamilyId(), clientType, deviceId, ipAddress);

        return TokenResponse.of(newAccessToken, newRefreshToken, accessTokenExpiryMinutes * 60);
    }

    @Transactional
    public void revokeToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) return;
        String hash = sha256(rawRefreshToken);
        refreshTokenRepo.findByTokenHash(hash).ifPresent(t -> {
            t.setRevoked(true);
            refreshTokenRepo.save(t);
        });
    }

    @Transactional
    public void revokeAllForUser(String tenantId, String userId) {
        refreshTokenRepo.revokeAllForUser(tenantId, userId);
    }

    public String createOnboardingToken(String provider, String sub, String email, boolean emailVerified, String name) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("provider", provider);
            map.put("sub", sub);
            map.put("email", email);
            map.put("emailVerified", emailVerified);
            map.put("name", name != null ? name : "");
            map.put("exp", Instant.now().plus(15, ChronoUnit.MINUTES).getEpochSecond());

            String payload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(map));
            String sig = signHmac(payload);
            return payload + "." + sig;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create onboarding token", e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> parseOnboardingToken(String token) {
        try {
            int dot = token.lastIndexOf('.');
            if (dot <= 0) {
                throw new MedShopException.BadRequestException("Invalid onboarding token format");
            }
            String payload = token.substring(0, dot);
            String sig = token.substring(dot + 1);

            if (!MessageDigest.isEqual(signHmac(payload).getBytes(StandardCharsets.UTF_8), sig.getBytes(StandardCharsets.UTF_8))) {
                throw new MedShopException.BadRequestException("Invalid onboarding token signature");
            }

            byte[] jsonBytes = Base64.getUrlDecoder().decode(payload);
            Map<String, Object> map = objectMapper.readValue(jsonBytes, Map.class);

            Number exp = (Number) map.get("exp");
            if (exp == null || Instant.now().getEpochSecond() > exp.longValue()) {
                throw new MedShopException.BadRequestException("Onboarding token has expired");
            }

            return map;
        } catch (MedShopException e) {
            throw e;
        } catch (Exception e) {
            throw new MedShopException.BadRequestException("Failed to parse onboarding token: " + e.getMessage());
        }
    }

    private String signHmac(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(onboardingSigningKey, "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

