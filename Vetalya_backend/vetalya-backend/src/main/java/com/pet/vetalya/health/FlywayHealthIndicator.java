package com.pet.vetalya.health;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Health check verifying Flyway database schema migration status and current version.
 */
@Component("flywayMigration")
public class FlywayHealthIndicator extends CachedHealthIndicator {

    private final Flyway flyway;

    public FlywayHealthIndicator(@Autowired(required = false) Flyway flyway) {
        super(Duration.ofSeconds(30));
        this.flyway = flyway;
    }

    @Override
    protected Health doCheck() {
        if (flyway == null) {
            return Health.up().withDetail("flyway", "Flyway not enabled in this profile").build();
        }
        try {
            MigrationInfo current = flyway.info().current();
            if (current != null) {
                return Health.up()
                        .withDetail("latestVersion", current.getVersion() != null ? current.getVersion().getVersion() : "baseline")
                        .withDetail("description", current.getDescription())
                        .withDetail("state", current.getState().name())
                        .withDetail("installedOn", current.getInstalledOn() != null ? current.getInstalledOn().toString() : "unknown")
                        .build();
            }
            return Health.up().withDetail("flyway", "No migrations applied yet").build();
        } catch (Exception ex) {
            return Health.down(ex)
                    .withDetail("error", "Failed to retrieve Flyway migration state: " + ex.getMessage())
                    .build();
        }
    }
}
