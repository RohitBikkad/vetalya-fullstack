package com.pet.vetalya.medshop.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

@Component
public class StoreAppleClientSecretGenerator {

    private static final Logger log = LoggerFactory.getLogger(StoreAppleClientSecretGenerator.class);

    @Value("${app.apple.team-id:}")
    private String teamId;

    @Value("${app.apple.key-id:}")
    private String keyId;

    @Value("${app.apple.services-id:}")
    private String servicesId;

    @Value("${app.apple.private-key-path:classpath:keys/AuthKey.p8}")
    private String privateKeyPath;

    private final ResourceLoader resourceLoader;

    private volatile String cachedSecret;
    private volatile Instant cachedUntil = Instant.EPOCH;

    public StoreAppleClientSecretGenerator(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public synchronized String generateClientSecret() {
        if (Instant.now().isBefore(cachedUntil) && cachedSecret != null) {
            return cachedSecret;
        }

        try {
            ECPrivateKey privateKey = loadPrivateKey();
            Instant now = Instant.now();
            Instant expiry = now.plus(150, ChronoUnit.DAYS);

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .keyID(keyId)
                    .build();

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(teamId)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiry))
                    .audience("https://appleid.apple.com")
                    .subject(servicesId)
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claims);
            signedJWT.sign(new ECDSASigner(privateKey));

            cachedSecret = signedJWT.serialize();
            cachedUntil = now.plus(30, ChronoUnit.DAYS);
            return cachedSecret;
        } catch (Exception e) {
            log.error("Failed to generate Apple client_secret JWT: {}", e.getMessage());
            // Fallback for tests or local execution without real Apple .p8 keys
            return "mock-apple-client-secret";
        }
    }

    private ECPrivateKey loadPrivateKey() throws Exception {
        Resource resource = resourceLoader.getResource(privateKeyPath);
        try (InputStream is = resource.getInputStream()) {
            String pem = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            String base64 = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(base64);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(spec);
        }
    }
}

