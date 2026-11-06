package com.pet.vetalya.auth.service;

import com.pet.vetalya.config.AppProperties;
import com.pet.vetalya.dto.ProviderUserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Validates Google and Apple ID tokens received directly from native mobile SDKs
 * (Sign in with Google on Android/iOS and Sign in with Apple on iOS).
 * Validates cryptographic signature using the provider's JWK set, issuer, and audience.
 */
@Service
public class NativeTokenVerificationService {

    private static final Logger log = LoggerFactory.getLogger(NativeTokenVerificationService.class);

    private final AppProperties appProperties;
    private final JwtDecoder googleDecoder;
    private final JwtDecoder appleDecoder;

    public NativeTokenVerificationService(AppProperties appProperties) {
        this.appProperties = appProperties;
        this.googleDecoder = NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .jwsAlgorithms(algs -> algs.add(SignatureAlgorithm.RS256))
                .build();

        this.appleDecoder = NimbusJwtDecoder.withJwkSetUri("https://appleid.apple.com/auth/keys")
                .jwsAlgorithms(algs -> {
                    algs.add(SignatureAlgorithm.ES256);
                    algs.add(SignatureAlgorithm.RS256);
                })
                .build();
    }

    public ProviderUserInfo verifyGoogleToken(String idToken) {
        try {
            Jwt jwt = googleDecoder.decode(idToken);

            // Validate issuer
            String iss = jwt.getClaimAsString("iss");
            if (!"https://accounts.google.com".equals(iss) && !"accounts.google.com".equals(iss)) {
                throw new IllegalArgumentException("Invalid Google token issuer: " + iss);
            }

            // Validate audience
            List<String> aud = jwt.getAudience();
            List<String> allowedClientIds = appProperties.google() != null ? appProperties.google().clientIds() : null;
            if (allowedClientIds != null && !allowedClientIds.isEmpty()) {
                boolean matches = aud != null && aud.stream().anyMatch(a -> allowedClientIds.stream().anyMatch(c -> c != null && c.equals(a)));
                if (!matches) {
                    log.warn("Google token aud {} does not match configured client IDs {}", aud, allowedClientIds);
                    throw new IllegalArgumentException("Invalid Google token audience");
                }
            }

            String sub = jwt.getSubject();
            String email = jwt.getClaimAsString("email");
            Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");
            String givenName = jwt.getClaimAsString("given_name");
            String familyName = jwt.getClaimAsString("family_name");
            String picture = jwt.getClaimAsString("picture");

            return new ProviderUserInfo(
                    "GOOGLE",
                    sub,
                    email,
                    Boolean.TRUE.equals(emailVerified),
                    givenName,
                    familyName,
                    picture
            );
        } catch (JwtException e) {
            log.error("Failed to verify Google ID token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid Google ID token: " + e.getMessage(), e);
        }
    }

    public ProviderUserInfo verifyAppleToken(String idToken, String rawName) {
        try {
            Jwt jwt = appleDecoder.decode(idToken);

            // Validate issuer
            String iss = jwt.getClaimAsString("iss");
            if (!"https://appleid.apple.com".equals(iss)) {
                throw new IllegalArgumentException("Invalid Apple token issuer: " + iss);
            }

            // Validate audience (bundle ID or services ID)
            List<String> aud = jwt.getAudience();
            String bundleId = appProperties.apple() != null ? appProperties.apple().bundleId() : null;
            String servicesId = appProperties.apple() != null ? appProperties.apple().servicesId() : null;

            boolean audMatches = aud != null && (
                    (bundleId != null && !bundleId.isBlank() && aud.contains(bundleId)) ||
                    (servicesId != null && !servicesId.isBlank() && aud.contains(servicesId))
            );

            if (!audMatches && ((bundleId != null && !bundleId.isBlank()) || (servicesId != null && !servicesId.isBlank()))) {
                log.warn("Apple token aud {} does not match bundleId {} or servicesId {}", aud, bundleId, servicesId);
                throw new IllegalArgumentException("Invalid Apple token audience");
            }

            String sub = jwt.getSubject();
            String email = jwt.getClaimAsString("email");

            Object emailVerifiedRaw = jwt.getClaim("email_verified");
            boolean emailVerified = parseEmailVerified(emailVerifiedRaw);

            // First-time login from iOS device can pass user's full name
            String firstName = null;
            String lastName = null;
            if (rawName != null && !rawName.isBlank()) {
                String[] parts = rawName.trim().split("\\s+", 2);
                firstName = parts[0];
                if (parts.length > 1) {
                    lastName = parts[1];
                }
            }

            return new ProviderUserInfo(
                    "APPLE",
                    sub,
                    email,
                    emailVerified,
                    firstName,
                    lastName,
                    null
            );
        } catch (JwtException e) {
            log.error("Failed to verify Apple ID token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid Apple ID token: " + e.getMessage(), e);
        }
    }

    private boolean parseEmailVerified(Object value) {
        if (value instanceof Boolean b) return b;
        if (value instanceof String s) return "true".equalsIgnoreCase(s);
        return false;
    }
}
