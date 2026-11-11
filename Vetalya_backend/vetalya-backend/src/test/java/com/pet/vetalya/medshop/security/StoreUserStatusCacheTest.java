package com.pet.vetalya.medshop.security;

import com.pet.vetalya.medshop.tenant.entity.Tenant;
import com.pet.vetalya.medshop.tenant.repository.TenantRepository;
import com.pet.vetalya.medshop.user.entity.StoreUser;
import com.pet.vetalya.medshop.user.repository.StoreUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoreUserStatusCacheTest {

    @Mock private StoreUserRepository userRepo;
    @Mock private TenantRepository tenantRepo;

    private StoreUserStatusCache statusCache;

    @BeforeEach
    void setUp() {
        statusCache = new StoreUserStatusCache(userRepo, tenantRepo);
    }

    @Test
    @DisplayName("Active user, active tenant and matching token version returns true")
    void testActiveUserAndTenantValid() {
        StoreUser user = new StoreUser();
        user.setId("user-1");
        user.setActive(true);
        user.setTokenVersion(2);
        when(userRepo.findByTenantIdAndId("tenant-1", "user-1")).thenReturn(Optional.of(user));

        Tenant tenant = new Tenant("tenant-1", "Shop 1", null, "27", "Pune", "9999999999", "s@s.com", "BASIC", false);
        tenant.setActive(true);
        when(tenantRepo.findById("tenant-1")).thenReturn(Optional.of(tenant));

        boolean valid = statusCache.isTokenValid("tenant-1", "user-1", 2);
        assertThat(valid).isTrue();

        // Second call hits in-memory cache without hitting database repositories again
        boolean cachedValid = statusCache.isTokenValid("tenant-1", "user-1", 2);
        assertThat(cachedValid).isTrue();
        verify(userRepo, times(1)).findByTenantIdAndId("tenant-1", "user-1");
    }

    @Test
    @DisplayName("Inactive/suspended user returns false (401)")
    void testInactiveUserInvalid() {
        StoreUser user = new StoreUser();
        user.setId("user-suspended");
        user.setActive(false); // SUSPENDED
        user.setTokenVersion(1);
        when(userRepo.findByTenantIdAndId("tenant-1", "user-suspended")).thenReturn(Optional.of(user));

        Tenant tenant = new Tenant("tenant-1", "Shop 1", null, "27", "Pune", "9999999999", "s@s.com", "BASIC", false);
        tenant.setActive(true);
        when(tenantRepo.findById("tenant-1")).thenReturn(Optional.of(tenant));

        boolean valid = statusCache.isTokenValid("tenant-1", "user-suspended", 1);
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Inactive/suspended tenant returns false (401)")
    void testInactiveTenantInvalid() {
        StoreUser user = new StoreUser();
        user.setId("user-1");
        user.setActive(true);
        user.setTokenVersion(1);
        when(userRepo.findByTenantIdAndId("tenant-1", "user-1")).thenReturn(Optional.of(user));

        Tenant tenant = new Tenant("tenant-1", "Shop 1", null, "27", "Pune", "9999999999", "s@s.com", "BASIC", false);
        tenant.setActive(false); // SUSPENDED TENANT
        when(tenantRepo.findById("tenant-1")).thenReturn(Optional.of(tenant));

        boolean valid = statusCache.isTokenValid("tenant-1", "user-1", 1);
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Older token version returns false when user token version is bumped (session invalidation)")
    void testBumpedTokenVersionInvalid() {
        StoreUser user = new StoreUser();
        user.setId("user-1");
        user.setActive(true);
        user.setTokenVersion(3); // Bumped to 3
        when(userRepo.findByTenantIdAndId("tenant-1", "user-1")).thenReturn(Optional.of(user));

        Tenant tenant = new Tenant("tenant-1", "Shop 1", null, "27", "Pune", "9999999999", "s@s.com", "BASIC", false);
        tenant.setActive(true);
        when(tenantRepo.findById("tenant-1")).thenReturn(Optional.of(tenant));

        // Token has ver = 2, but DB has tokenVersion = 3
        boolean valid = statusCache.isTokenValid("tenant-1", "user-1", 2);
        assertThat(valid).isFalse();
    }
}

