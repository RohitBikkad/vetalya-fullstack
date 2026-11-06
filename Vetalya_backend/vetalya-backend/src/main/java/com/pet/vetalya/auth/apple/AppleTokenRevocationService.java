package com.pet.vetalya.auth.apple;

import com.pet.vetalya.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Revokes an Apple refresh token using Apple's /auth/revoke endpoint.
 * Required by App Store Guideline 5.1.1(v) when deleting an account.
 */
@Service
public class AppleTokenRevocationService {

    private static final Logger log = LoggerFactory.getLogger(AppleTokenRevocationService.class);
    private static final String APPLE_REVOKE_URL = "https://appleid.apple.com/auth/revoke";

    private final AppProperties appProperties;
    private final AppleClientSecretGenerator secretGenerator;
    private final RestClient restClient;

    public AppleTokenRevocationService(AppProperties appProperties,
                                      AppleClientSecretGenerator secretGenerator) {
        this.appProperties = appProperties;
        this.secretGenerator = secretGenerator;
        this.restClient = RestClient.create();
    }

    /**
     * Revokes the Apple user's refresh token with Apple.
     * @param appleRefreshToken The refresh token stored during authorization
     */
    public void revokeToken(String appleRefreshToken) {
        if (appleRefreshToken == null || appleRefreshToken.isBlank()) {
            log.warn("Cannot revoke Apple token: provider refresh token is missing");
            return;
        }

        try {
            String clientSecret = secretGenerator.generateClientSecret();
            String clientId = appProperties.apple().servicesId();
            if (clientId == null || clientId.isBlank()) {
                clientId = appProperties.apple().bundleId();
            }

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("token", appleRefreshToken);
            body.add("token_type_hint", "refresh_token");

            restClient.post()
                    .uri(APPLE_REVOKE_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully revoked Apple token with Apple auth server");
        } catch (Exception e) {
            log.error("Failed to revoke Apple token at {}: {}", APPLE_REVOKE_URL, e.getMessage(), e);
            // Non-fatal or rethrow depending on application policy
        }
    }
}
