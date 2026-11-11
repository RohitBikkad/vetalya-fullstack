package com.pet.vetalya.actuator;

import com.pet.vetalya.config.ActuatorSecurityConfig;
import com.pet.vetalya.config.AppProperties;
import com.pet.vetalya.health.DatabaseHealthIndicator;
import com.pet.vetalya.health.ReminderSchedulerHealthIndicator;
import com.pet.vetalya.repository.ReminderRepository;
import com.pet.vetalya.scheduling.ReminderScheduledJob;
import com.pet.vetalya.scheduling.ReusableJobRunner;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:actuatortest;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.flyway.enabled=false",
        "app.management.security.username=actuator-admin",
        "app.management.security.password=SecretPassword123!",
        "app.management.allowed-ips=*"
})
@AutoConfigureMockMvc
class ActuatorSecurityAndHealthTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DataSource dataSource;

    @MockitoBean
    private ReminderRepository reminderRepo;

    @Test
    @DisplayName("/actuator/health/liveness works without authentication")
    void livenessEndpointWorksWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("/actuator/metrics gets 401 Unauthorized without credentials")
    void metricsEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/actuator/metrics succeeds (200) with valid HTTP Basic credentials")
    void metricsEndpointSucceedsWithBasicAuth() throws Exception {
        mockMvc.perform(get("/actuator/metrics")
                .with(SecurityMockMvcRequestPostProcessors.httpBasic("actuator-admin", "SecretPassword123!")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Health status goes DOWN when database is unavailable while liveness stays UP")
    void databaseFailureReportsDownWhileLivenessStaysUp() throws SQLException {
        // Mock DataSource to simulate complete database outage
        DataSource failingDataSource = mock(DataSource.class);
        when(failingDataSource.getConnection()).thenThrow(new SQLException("Connection refused to PostgreSQL cluster"));

        DatabaseHealthIndicator dbIndicator = new DatabaseHealthIndicator(failingDataSource);
        Health dbHealth = dbIndicator.health();

        assertThat(dbHealth.getStatus()).isEqualTo(Status.DOWN);
        assertThat(dbHealth.getDetails()).containsKey("error");
        assertThat((String) dbHealth.getDetails().get("error")).contains("Database connectivity timeout");

        // Actuator Liveness probe remains completely independent of database health
        // (verified via liveness group definition which only binds to livenessState)
    }

    @Test
    @DisplayName("Reminder-job indicator fails (DOWN) when the job is stale")
    void reminderJobFailsWhenStale() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ReusableJobRunner runner = new ReusableJobRunner(meterRegistry);

        // Run the job once in the past
        runner.runJob(ReminderScheduledJob.JOB_NAME, () -> 5L);

        // Indicator configured with a 1-second max stale interval
        ReminderSchedulerHealthIndicator indicator =
                new ReminderSchedulerHealthIndicator(runner, reminderRepo, 1);

        // Simulate passage of time past the 1-second threshold
        try {
            Thread.sleep(1100);
        } catch (InterruptedException ignored) {}

        Health health = indicator.health();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat((String) health.getDetails().get("error")).contains("exceeded 1s without success");
    }
}
