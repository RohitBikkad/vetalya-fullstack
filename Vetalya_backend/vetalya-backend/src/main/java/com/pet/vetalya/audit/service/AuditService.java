package com.pet.vetalya.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.vetalya.audit.entity.AuditLog;
import com.pet.vetalya.audit.repository.AuditLogRepository;
import com.pet.vetalya.logging.CorrelationIdFilter;
import com.pet.vetalya.metrics.AppMetrics;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executor;

/**
 * Production-grade AuditService for logging security and business events.
 * Executes audit persistence asynchronously through a bounded-queue executor,
 * safely handling errors without ever interrupting caller business workflows.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepo;
    private final Executor auditExecutor;
    private final AppMetrics appMetrics;
    private final ObjectMapper objectMapper;
    private final Set<String> trustedProxies;

    @org.springframework.beans.factory.annotation.Autowired
    public AuditService(
            AuditLogRepository auditLogRepo,
            @Qualifier("auditExecutor") Executor auditExecutor,
            AppMetrics appMetrics,
            @org.springframework.beans.factory.annotation.Autowired(required = false) ObjectMapper objectMapper,
            @Value("${app.security.trusted-proxies:127.0.0.1,0:0:0:0:0:0:0:1}") String trustedProxiesConfig) {
        this.auditLogRepo = auditLogRepo;
        this.auditExecutor = auditExecutor;
        this.appMetrics = appMetrics;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.trustedProxies = parseTrustedProxies(trustedProxiesConfig);
    }

    /**
     * Main non-blocking audit logging method.
     */
    public void recordEvent(
            String eventType,
            String userId,
            String role,
            String provider,
            String outcome,
            String reason,
            String clientType,
            String deviceId,
            String ipAddress,
            String userAgent,
            Map<String, Object> details) {

        String correlationId = CorrelationIdFilter.getCorrelationId();
        String jsonDetails = serializeDetails(details);
        String truncatedUserAgent = truncate(userAgent, 255);

        AuditLog entity = new AuditLog();
        entity.setEventType(truncate(eventType, 50));
        entity.setUserId(truncate(userId, 26));
        entity.setRole(truncate(role, 100));
        entity.setProvider(truncate(provider != null ? provider.toUpperCase() : null, 50));
        entity.setOutcome(truncate(outcome != null ? outcome.toUpperCase() : "FAILURE", 20));
        entity.setReason(truncate(reason, 100));
        entity.setClientType(truncate(clientType, 50));
        entity.setDeviceId(truncate(deviceId, 255));
        entity.setIpAddress(ipAddress);
        entity.setUserAgent(truncatedUserAgent);
        entity.setCorrelationId(correlationId);
        entity.setDetails(jsonDetails);
        entity.setCreatedAt(Instant.now());

        try {
            auditExecutor.execute(() -> {
                try {
                    auditLogRepo.save(entity);
                } catch (Exception ex) {
                    log.error("Failed to persist audit log entry for event {} [correlationId={}]: {}",
                            eventType, correlationId, ex.getMessage());
                    appMetrics.recordAuditWriteFailed("db_error");
                }
            });
        } catch (Exception ex) {
            log.error("Failed to dispatch audit log entry for event {} to executor: {}", eventType, ex.getMessage());
            appMetrics.recordAuditWriteFailed("dispatch_error");
        }
    }

    /**
     * Resolves client IP address taking into account trusted forward proxy headers.
     */
    public String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String remoteAddr = request.getRemoteAddr();
        if (isTrustedProxy(remoteAddr)) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",")[0].trim();
            }
        }
        return remoteAddr;
    }

    public String resolveClientType(HttpServletRequest request) {
        if (request == null) return null;
        return request.getHeader("X-Client-Type");
    }

    public String resolveDeviceId(HttpServletRequest request) {
        if (request == null) return null;
        return request.getHeader("X-Device-Id");
    }

    public String resolveUserAgent(HttpServletRequest request) {
        if (request == null) return null;
        return request.getHeader("User-Agent");
    }

    private boolean isTrustedProxy(String ip) {
        if (ip == null) return false;
        return trustedProxies.contains(ip.trim());
    }

    private Set<String> parseTrustedProxies(String config) {
        if (config == null || config.isBlank()) {
            return Collections.emptySet();
        }
        Set<String> set = new HashSet<>();
        for (String part : config.split(",")) {
            if (!part.isBlank()) {
                set.add(part.trim());
            }
        }
        return Collections.unmodifiableSet(set);
    }

    private String serializeDetails(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize audit details map to JSON: {}", e.getMessage());
            return "{}";
        }
    }

    private String truncate(String val, int maxLen) {
        if (val == null) return null;
        return val.length() <= maxLen ? val : val.substring(0, maxLen);
    }
}

