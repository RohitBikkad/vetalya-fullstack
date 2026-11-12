package com.pet.vetalya.medshop.security;

import com.pet.vetalya.medshop.tenant.entity.Tenant;
import com.pet.vetalya.medshop.tenant.repository.TenantRepository;
import com.pet.vetalya.medshop.user.entity.StoreUser;
import com.pet.vetalya.medshop.user.repository.StoreUserRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance 30-second in-memory cache for user and tenant status checks.
 * Avoids issuing 2 SELECT queries per REST request while guaranteeing revoked or demoted
 * users are locked out within 30 seconds at most.
 */
@Component
public class StoreUserStatusCache {

    public record UserStatus(
            boolean userActive,
            boolean tenantActive,
            int tokenVersion,
            Instant cachedAt
    ) {
        public boolean isExpired(long ttlSeconds) {
            return Instant.now().isAfter(cachedAt.plusSeconds(ttlSeconds));
        }
    }

    private final ConcurrentHashMap<String, UserStatus> cache = new ConcurrentHashMap<>();
    private final StoreUserRepository userRepo;
    private final TenantRepository tenantRepo;
    private final long ttlSeconds = 30;

    public StoreUserStatusCache(StoreUserRepository userRepo, TenantRepository tenantRepo) {
        this.userRepo = userRepo;
        this.tenantRepo = tenantRepo;
    }

    public boolean isTokenValid(String tenantId, String userId, Integer tokenVersion) {
        if (userId == null || tenantId == null) {
            return false;
        }

        UserStatus status = cache.compute(userId, (id, existing) -> {
            if (existing != null && !existing.isExpired(ttlSeconds)) {
                return existing;
            }
            StoreUser user = userRepo.findByTenantIdAndId(tenantId, userId).orElse(null);
            if (user == null) {
                return new UserStatus(false, false, -1, Instant.now());
            }
            Tenant tenant = tenantRepo.findById(tenantId).orElse(null);
            boolean tenantActive = tenant != null && tenant.isActive();
            return new UserStatus(user.isActive(), tenantActive, user.getTokenVersion(), Instant.now());
        });

        if (!status.userActive() || !status.tenantActive()) {
            return false;
        }
        if (tokenVersion != null && status.tokenVersion() != tokenVersion) {
            return false;
        }
        return true;
    }

    public void evict(String userId) {
        if (userId != null) {
            cache.remove(userId);
        }
    }
}

