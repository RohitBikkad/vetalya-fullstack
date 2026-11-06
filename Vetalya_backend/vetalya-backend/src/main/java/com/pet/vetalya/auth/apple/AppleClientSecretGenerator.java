package com.pet.vetalya.auth.apple;

import com.pet.vetalya.config.AppProperties;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

/**
 * Generates the short-lived JWT that Apple requires as client_secret.
 * Signed with ES256 using the .p8 private key downloaded from Apple Developer.
 *
 * The generated JWT is cached for 30 days (Apple allows up to 6 months).
 */
@Component
public class AppleClientSecretGenerator {

    private static final Logger log = LoggerFactory.getLogger(AppleClientSecretGenerator.class);
    private static final long CACHE_DURATION_SECONDS = 30L * 24 * 60 * 60; // 30 days

    private final AppProperties appProperties;
    private final ResourceLoader resourceLoader;

    private volatile String cachedSecret;
    private volatile Instant cachedUntil = Instant.EPOCH;

    public AppleClientSecretGenerator(AppProperties appProperties, ResourceLoader resourceLoader) {
        this.appProperties = appProperties;
        this.resourceLoader = resourceLoader;
    }

    public String generateClientSecret() {
        if (Instant.now().isBefore(cachedUntil) && cachedSecret != null) {
            return cachedSecret;
        }

        try {
            ECPrivateKey privateKey = loadPrivateKey();
            AppProperties.Apple apple = appProperties.apple();

            Instant now = Instant.now();
            // Apple allows up to 6 months, we use ~5 months for safety
            Instant expiry = now.plus(150, ChronoUnit.DAYS);

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .keyID(apple.keyId())
                    .build();

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(apple.teamId())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiry))
                    .audience("https://appleid.apple.com")
                    .subject(apple.servicesId())
                    .build();

            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(new ECDSASigner(privateKey));

            cachedSecret = jwt.serialize();
            cachedUntil = now.plusSeconds(CACHE_DURATION_SECONDS);

            log.debug("Generated new Apple client_secret, valid until {}", expiry);
            return cachedSecret;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Apple client secret", e);
        }
    }

    private ECPrivateKey loadPrivateKey() throws Exception {
        String keyPath = appProperties.apple().privateKeyPath();
        String pemContent;

        try (InputStream is = resourceLoader.getResource(keyPath).getInputStream()) {
            pemContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        // Strip PEM headers/footers and whitespace
        String base64Key = pemContent
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("EC");
        return (ECPrivateKey) kf.generatePrivate(spec);
    }
}
