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
        Cors cors,
        Appointment appointment,
        Pet pet,
        Review review,
        String zone
) {
    public AppProperties {
        if (zone == null || zone.isBlank()) {
            zone = "Asia/Kolkata";
        }
        if (jwt == null) {
            jwt = new Jwt("classpath:keys/private.pem", "classpath:keys/public.pem", 1440, 14);
        }
        if (cors == null) {
            cors = new Cors("http://localhost:3000");
        }
        if (google == null) {
            google = new Google(List.of("1030204623930-s3t2s597p2rvf5nal0i3552vh8t10h23.apps.googleusercontent.com"));
        }
        if (apple == null) {
            apple = new Apple("", "", "", "", "classpath:keys/AuthKey.p8");
        }
        if (appointment == null) {
            appointment = new Appointment(30, 60, 2, List.of(24, 1));
        }
        if (pet == null) {
            pet = new Pet(20);
        }
        if (review == null) {
            review = new Review(30);
        }
    }

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

    public record Cors(String allowedOrigin) {
        /** Exact browser origins only. Native apps use Bearer tokens and do not require CORS origins. */
        public List<String> allowedOrigins() {
            if (allowedOrigin == null || allowedOrigin.isBlank()) return List.of();
            return java.util.Arrays.stream(allowedOrigin.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).distinct().map(origin -> {
                    try {
                        var uri = java.net.URI.create(origin);
                        if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getHost() == null
                            || uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null
                            || (uri.getRawPath() != null && !uri.getRawPath().isEmpty()) || origin.contains("*"))
                            throw new IllegalArgumentException();
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("CORS_ALLOWED_ORIGINS must contain exact http(s) origins without paths or wildcards");
                    }
                    return origin;
                }).toList();
        }
    }

    public record Appointment(
            int slotMinutes,
            int minLeadMinutes,
            int cancelCutoffHours,
            List<Integer> reminderOffsetsHours
    ) {
        public Appointment {
            if (slotMinutes <= 0) slotMinutes = 30;
            if (minLeadMinutes <= 0) minLeadMinutes = 60;
            if (cancelCutoffHours <= 0) cancelCutoffHours = 2;
            if (reminderOffsetsHours == null || reminderOffsetsHours.isEmpty()) {
                reminderOffsetsHours = List.of(24, 1);
            }
        }
    }

    public record Pet(
            int maxPerOwner
    ) {
        public Pet {
            if (maxPerOwner <= 0) maxPerOwner = 20;
        }
    }

    public record Review(
            int editWindowDays
    ) {
        public Review {
            if (editWindowDays <= 0) editWindowDays = 30;
        }
    }
}
