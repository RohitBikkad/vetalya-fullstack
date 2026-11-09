package com.pet.vetalya.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

import java.time.Duration;
import java.time.Instant;

/**
 * Base health indicator implementing cached health status results to protect
 * downstream systems and dependencies from health-check request floods.
 */
public abstract class CachedHealthIndicator implements HealthIndicator {

    private final Duration cacheDuration;
    private volatile Health cachedHealth;
    private volatile Instant lastChecked = Instant.MIN;

    protected CachedHealthIndicator(Duration cacheDuration) {
        this.cacheDuration = cacheDuration;
    }

    @Override
    public Health health() {
        Instant now = Instant.now();
        if (cachedHealth != null && Duration.between(lastChecked, now).compareTo(cacheDuration) < 0) {
            return cachedHealth;
        }
        synchronized (this) {
            if (cachedHealth != null && Duration.between(lastChecked, now).compareTo(cacheDuration) < 0) {
                return cachedHealth;
            }
            cachedHealth = doCheck();
            lastChecked = Instant.now();
            return cachedHealth;
        }
    }

    public void clearCache() {
        this.cachedHealth = null;
        this.lastChecked = Instant.MIN;
    }

    protected abstract Health doCheck();
}
