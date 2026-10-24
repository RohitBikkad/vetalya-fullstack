package com.pet.vetalya.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Public health and status endpoints to verify standalone server operation without authentication.
 */
@RestController
public class SystemController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        return ResponseEntity.ok(Map.of(
                "app", "Vetalya API",
                "status", "RUNNING",
                "version", "0.0.1-SNAPSHOT",
                "timestamp", Instant.now().toString(),
                "endpoints", Map.of(
                        "googleLogin", "/oauth2/authorization/google?role=ROLE_PET_OWNER&client=web",
                        "appleLogin", "/oauth2/authorization/apple?role=ROLE_PET_OWNER&client=web",
                        "refresh", "/api/auth/refresh",
                        "exchange", "/api/auth/exchange"
                )
        ));
    }

    @GetMapping("/api/public/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "database", "CONNECTED"
        ));
    }
}
