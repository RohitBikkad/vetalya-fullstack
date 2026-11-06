package com.pet.vetalya.auth.service;

import com.pet.vetalya.config.AppProperties;
import com.pet.vetalya.entity.RefreshToken;
import com.pet.vetalya.entity.UserAuth;
import com.pet.vetalya.repository.RefreshTokenRepository;
import com.pet.vetalya.repository.UserAuthRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private final JwtEncoder jwtEncoder;
    private final AppProperties appProperties;
    private final RefreshTokenRepository refreshTokenRepo;
    private final UserAuthRepository userAuthRepo;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenService(JwtEncoder jwtEncoder,
                        AppProperties appProperties,
                        RefreshTokenRepository refreshTokenRepo,
                        UserAuthRepository userAuthRepo) {
        this.jwtEncoder = jwtEncoder;
        this.appProperties = appProperties;
        this.refreshTokenRepo = refreshTokenRepo;
        this.userAuthRepo = userAuthRepo;
    }

    /** Issue a JWT access token with RS256. */
    public String createAccessToken(UserAuth user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("vetalya")
                .subject(user.getUserId())
                .issuedAt(now)
                .expiresAt(now.plus(appProperties.jwt().accessTokenExpiryMinutes(), ChronoUnit.MINUTES))
                .claim("roles", List.of(user.getRole()))
                .claim("ver", user.getTokenVersion())
                .build();

        JwsHeader header = JwsHeader.with(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /**
     * Create an opaque refresh token.
     * Returns the raw token string (to send to client). Only the SHA-256 hash is stored.
     */
    public String createRefreshToken(UserAuth user, String clientType, String deviceId, String ipAddress) {
        String rawToken = generateSecureToken();

        RefreshToken entity = new RefreshToken();
        entity.setUserId(user.getUserId());
        entity.setTokenHash(sha256(rawToken));
        entity.setFamilyId(UUID.randomUUID());
        entity.setClientType(clientType);
        entity.setDeviceId(deviceId);
        entity.setIpAddress(ipAddress);
        entity.setExpiresAt(Instant.now().plus(appProperties.jwt().refreshTokenExpiryDays(), ChronoUnit.DAYS));
        refreshTokenRepo.save(entity);

        return rawToken;
    }

    /**
     * Rotate a refresh token: mark old as used, issue a new one in the same family.
     * If the old token was already used (reuse detected), revoke the entire family.
     */
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public RotationResult rotateRefreshToken(String rawOldToken, String ipAddress) {
        String hash = sha256(rawOldToken);
        RefreshToken old = refreshTokenRepo.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (old.isRevoked()) {
            throw new InvalidTokenException("Refresh token revoked");
        }

        if (old.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token expired");
        }

        // Reuse detection: if the token was already used, someone stole it
        if (old.getUsedAt() != null) {
            log.warn("Refresh token reuse detected for user {}. Revoking family {}", old.getUserId(), old.getFamilyId());
            refreshTokenRepo.revokeFamily(old.getFamilyId());
            throw new InvalidTokenException("Refresh token reuse detected — all tokens in this family revoked");
        }

        // Mark old token as used
        old.setUsedAt(Instant.now());

        UserAuth user = userAuthRepo.findById(old.getUserId())
                .orElseThrow(() -> new InvalidTokenException("User not found"));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new InvalidTokenException("Account is " + user.getStatus().toLowerCase());
        }

        // Issue new refresh token in the same family
        String newRawToken = generateSecureToken();
        RefreshToken newEntity = new RefreshToken();
        newEntity.setUserId(user.getUserId());
        newEntity.setTokenHash(sha256(newRawToken));
        newEntity.setFamilyId(old.getFamilyId());
        newEntity.setClientType(old.getClientType());
        newEntity.setDeviceId(old.getDeviceId());
        newEntity.setIpAddress(ipAddress);
        newEntity.setExpiresAt(Instant.now().plus(appProperties.jwt().refreshTokenExpiryDays(), ChronoUnit.DAYS));
        refreshTokenRepo.save(newEntity);

        String accessToken = createAccessToken(user);
        return new RotationResult(accessToken, newRawToken, user);
    }

    @Transactional
    public void revokeRefreshToken(String rawToken) {
        String hash = sha256(rawToken);
        refreshTokenRepo.findByTokenHash(hash).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepo.save(rt);
        });
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public record RotationResult(String accessToken, String refreshToken, UserAuth user) {}

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) { super(message); }
    }
}
