package com.pet.vetalya.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Duration;

/**
 * Health indicator for external OAuth identity providers (Google and Apple JWKS public key endpoints).
 * Configured as a non-critical dependency: failures report status "DEGRADED" and are excluded
 * from the readiness health group so that existing logged-in users are never impacted by external IdP blips.
 */
@Component("oauthJwks")
public class JwksHealthIndicator extends CachedHealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(JwksHealthIndicator.class);

    public static final Status DEGRADED = new Status("DEGRADED", "External JWKS endpoint degraded");
    private static final String GOOGLE_JWKS = "https://www.googleapis.com/oauth2/v3/certs";
    private static final String APPLE_JWKS = "https://appleid.apple.com/auth/keys";
    private static final int TIMEOUT_MS = 2000;

    public JwksHealthIndicator() {
        super(Duration.ofSeconds(30));
    }

    @Override
    protected Health doCheck() {
        boolean googleOk = probeEndpoint(GOOGLE_JWKS);
        boolean appleOk = probeEndpoint(APPLE_JWKS);

        Health.Builder builder;
        if (googleOk && appleOk) {
            builder = Health.up()
                    .withDetail("googleJwks", "REACHABLE")
                    .withDetail("appleJwks", "REACHABLE");
        } else {
            builder = Health.status(DEGRADED)
                    .withDetail("googleJwks", googleOk ? "REACHABLE" : "UNREACHABLE")
                    .withDetail("appleJwks", appleOk ? "REACHABLE" : "UNREACHABLE")
                    .withDetail("note", "External IdP reachability degraded. Existing JWT sessions continue unaffected.");
        }

        return builder.build();
    }

    private boolean probeEndpoint(String endpoint) {
        try {
            URL url = URI.create(endpoint).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            int responseCode = conn.getResponseCode();
            return responseCode >= 200 && responseCode < 400;
        } catch (Exception e) {
            log.debug("JWKS probe for {} failed: {}", endpoint, e.getMessage());
            return false;
        }
    }
}
