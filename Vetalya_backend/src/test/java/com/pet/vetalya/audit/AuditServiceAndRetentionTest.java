package com.pet.vetalya.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.vetalya.audit.entity.AuditLog;
import com.pet.vetalya.audit.repository.AuditLogRepository;
import com.pet.vetalya.audit.scheduler.AuditRetentionJob;
import com.pet.vetalya.audit.service.AuditService;
import com.pet.vetalya.metrics.AppMetrics;
import com.pet.vetalya.scheduling.ReusableJobRunner;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditServiceAndRetentionTest {

    @Test
    @DisplayName("Audit write failure does not break caller request and records failure metric")
    void auditWriteFailureDoesNotBreakCaller() {
        AuditLogRepository failingRepo = mock(AuditLogRepository.class);
        when(failingRepo.save(any(AuditLog.class))).thenThrow(new RuntimeException("Deadlock or DB timeout"));

        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AppMetrics appMetrics = new AppMetrics(meterRegistry);
        Executor directExecutor = Runnable::run; // Synchronous test execution

        AuditService auditService = new AuditService(
                failingRepo,
                directExecutor,
                appMetrics,
                new ObjectMapper(),
                "127.0.0.1");

        // Crucial requirement: AuditService must NEVER throw an exception back to
        // caller
        assertThatCode(() -> auditService.recordEvent(
                "APPOINTMENT_STATUS_CHANGED",
                "user_123",
                "ROLE_VET",
                null,
                "SUCCESS",
                null,
                "web",
                "device_abc",
                "192.168.1.1",
                "Mozilla/5.0",
                Map.of("appointmentId", "apt_001"))).doesNotThrowAnyException();

        // Metric must count the failed audit write
        double failedWrites = meterRegistry.counter("audit.write.failed", "reason", "db_error").count();
        assertThat(failedWrites).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Audit retention job deletes only old rows and preserves recent ones")
    void auditRetentionJobDeletesOnlyOldRows() {
        AuditLogRepository repo = mock(AuditLogRepository.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ReusableJobRunner runner = new ReusableJobRunner(meterRegistry);

        AuditRetentionJob retentionJob = new AuditRetentionJob(repo, runner, 180, 500);

        // Simulate that there are 3 old IDs older than 180 days on first fetch, and
        // none on second
        List<Long> expiredIds = List.of(101L, 102L, 103L);
        when(repo.findIdsOlderThan(any(Instant.class), any(Pageable.class)))
                .thenReturn(expiredIds)
                .thenReturn(List.of());

        when(repo.deleteByIdIn(expiredIds)).thenReturn(3);

        long deletedCount = retentionJob.deleteExpiredAuditLogs();

        assertThat(deletedCount).isEqualTo(3);
        verify(repo, times(1)).deleteByIdIn(expiredIds);
    }
}
