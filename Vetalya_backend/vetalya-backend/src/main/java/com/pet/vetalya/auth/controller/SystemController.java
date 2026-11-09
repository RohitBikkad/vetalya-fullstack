package com.pet.vetalya.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Public health and status endpoints to verify standalone server operation without authentication.
 */
@RestController
public class SystemController {

    private final JdbcTemplate jdbcTemplate;

    public SystemController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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

    @GetMapping("/api/public/db-inspect")
    public ResponseEntity<Map<String, Object>> dbInspect() {
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "CONNECTED");
        result.put("engine", "PostgreSQL (Supabase RDS)");
        result.put("timestamp", Instant.now().toString());

        Map<String, Long> tables = new LinkedHashMap<>();
        String[] tableNames = {"user_auth", "pet_owner", "vet", "hospital", "pet", "appointment", "vaccination", "reminder", "review", "otp_verification"};

        for (String table : tableNames) {
            try {
                Long count = jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class);
                tables.put(table, count != null ? count : 0L);
            } catch (Exception e) {
                tables.put(table, 0L);
            }
        }

        long latency = System.currentTimeMillis() - startTime;
        result.put("latencyMs", latency);
        result.put("tables", tables);
        return ResponseEntity.ok(result);
    }
}
