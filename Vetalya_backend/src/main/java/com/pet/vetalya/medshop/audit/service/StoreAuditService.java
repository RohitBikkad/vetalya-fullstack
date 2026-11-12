package com.pet.vetalya.medshop.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.vetalya.medshop.audit.entity.StoreAuditLog;
import com.pet.vetalya.medshop.audit.repository.StoreAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
public class StoreAuditService {

    private static final Logger log = LoggerFactory.getLogger(StoreAuditService.class);
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "password_hash", "passwordHash", "token", "refreshToken",
            "accessToken", "secret", "privateKey", "onboardingToken"
    );

    private final StoreAuditLogRepository auditRepo;
    private final ObjectMapper objectMapper;

    public StoreAuditService(StoreAuditLogRepository auditRepo, ObjectMapper objectMapper) {
        this.auditRepo = auditRepo;
        this.objectMapper = objectMapper;
    }

    @Async
    public void record(String tenantId, String userId, String entityType, String entityId,
                       String action, Object oldVal, Object newVal) {
        record(tenantId, userId, entityType, entityId, action, oldVal, newVal, null);
    }

    @Async
    public void record(String tenantId, String userId, String entityType, String entityId,
                       String action, Object oldVal, Object newVal, String ipAddress) {
        try {
            String oldJson = serializeSanitized(oldVal);
            String newJson = serializeSanitized(newVal);

            StoreAuditLog logEntry = new StoreAuditLog(
                    tenantId, userId, entityType, entityId, action, oldJson, newJson, ipAddress);
            auditRepo.save(logEntry);
        } catch (Exception e) {
            log.error("Failed to record store audit log for {}:{} [{}]: {}", entityType, entityId, action, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String serializeSanitized(Object val) {
        if (val == null) return null;
        try {
            if (val instanceof String str) return str;
            Map<String, Object> map = objectMapper.convertValue(val, Map.class);
            Map<String, Object> sanitized = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (SENSITIVE_KEYS.contains(entry.getKey())) {
                    sanitized.put(entry.getKey(), "[REDACTED]");
                } else {
                    sanitized.put(entry.getKey(), entry.getValue());
                }
            }
            return objectMapper.writeValueAsString(sanitized);
        } catch (Exception e) {
            return "{}";
        }
    }
}

