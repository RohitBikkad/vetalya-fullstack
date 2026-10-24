package com.pet.vetalya.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String webRedirectUrl,
        String mobileRedirectScheme,
        Jwt jwt,
        Google google,
        Apple apple,
        Cors cors
) {
    public record Jwt(
            String privateKeyPath,
            String publicKeyPath,
            int accessTokenExpiryMinutes,
            int refreshTokenExpiryDays
    ) {}

    public record Google(
            List<String> clientIds
    ) {}

    public record Apple(
            String teamId,
            String keyId,
            String servicesId,
            String bundleId,
            String privateKeyPath
    ) {}

    public record Cors(
            String allowedOrigin
    ) {}
}
