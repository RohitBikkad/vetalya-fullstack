package com.pet.vetalya.medshop.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StoreSecurityAndIsolationTest {

    @Test
    @DisplayName("AudienceValidator rejects tokens without 'medshop-api' audience")
    void testAudienceValidatorRejection() {
        StoreJwtConfig.AudienceValidator validator = new StoreJwtConfig.AudienceValidator("medshop-api");

        // Veterinary token with vet audience
        Jwt vetToken = new Jwt(
                "mock-token-value",
                Instant.now(),
                Instant.now().plusSeconds(900),
                Map.of("alg", "RS256"),
                Map.of("sub", "vet-user-1", "aud", List.of("vetalya-app"), "iss", "vetalya")
        );

        OAuth2TokenValidatorResult result = validator.validate(vetToken);
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).anyMatch(e -> e.getDescription().contains("medshop-api"));
    }

    @Test
    @DisplayName("AudienceValidator accepts tokens with 'medshop-api' audience")
    void testAudienceValidatorSuccess() {
        StoreJwtConfig.AudienceValidator validator = new StoreJwtConfig.AudienceValidator("medshop-api");

        Jwt storeToken = new Jwt(
                "mock-token-value",
                Instant.now(),
                Instant.now().plusSeconds(900),
                Map.of("alg", "RS256"),
                Map.of("sub", "store-user-1", "aud", List.of("medshop-api"), "iss", "medshop", "tenant_id", "shop-1")
        );

        OAuth2TokenValidatorResult result = validator.validate(storeToken);
        assertThat(result.hasErrors()).isFalse();
    }
}

