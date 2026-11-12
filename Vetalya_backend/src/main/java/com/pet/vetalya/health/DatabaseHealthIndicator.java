package com.pet.vetalya.health;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.springframework.boot.health.contributor.Health;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.Duration;

/**
 * Health check verifying live database query responsiveness and HikariCP pool capacity.
 */
@Component("databaseConnectivity")
public class DatabaseHealthIndicator extends CachedHealthIndicator {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public DatabaseHealthIndicator(DataSource dataSource) {
        super(Duration.ofSeconds(15));
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcTemplate.setQueryTimeout(3); // 3-second timeout
    }

    @Override
    protected Health doCheck() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (result == null || result != 1) {
                return Health.down().withDetail("error", "Unexpected query response").build();
            }

            Health.Builder builder = Health.up().withDetail("database", "PostgreSQL Responsive");

            if (dataSource instanceof HikariDataSource hikari) {
                HikariPoolMXBean poolBean = hikari.getHikariPoolMXBean();
                if (poolBean != null) {
                    int active = poolBean.getActiveConnections();
                    int idle = poolBean.getIdleConnections();
                    int pending = poolBean.getThreadsAwaitingConnection();
                    int total = poolBean.getTotalConnections();

                    builder.withDetail("activeConnections", active)
                            .withDetail("idleConnections", idle)
                            .withDetail("pendingThreads", pending)
                            .withDetail("totalConnections", total)
                            .withDetail("maxPoolSize", hikari.getMaximumPoolSize());

                    if (pending > 0) {
                        builder.withDetail("warning", "Threads awaiting database connection");
                    }
                }
            }
            return builder.build();
        } catch (Exception ex) {
            return Health.down(ex)
                    .withDetail("error", "Database connectivity timeout or error: " + ex.getMessage())
                    .build();
        }
    }
}
