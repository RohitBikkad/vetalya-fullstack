package com.pet.vetalya.audit.scheduler;

import com.pet.vetalya.audit.repository.AuditLogRepository;
import com.pet.vetalya.scheduling.ReusableJobRunner;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled job to prune expired audit logs older than the configured retention policy in batches.
 */
@Component
public class AuditRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(AuditRetentionJob.class);

    private final AuditLogRepository auditLogRepo;
    private final ReusableJobRunner jobRunner;
    private final int retentionDays;
    private final int batchSize;

    public AuditRetentionJob(
            AuditLogRepository auditLogRepo,
            ReusableJobRunner jobRunner,
            @Value("${app.audit.retention-days:180}") int retentionDays,
            @Value("${app.audit.batch-size:1000}") int batchSize) {
        this.auditLogRepo = auditLogRepo;
        this.jobRunner = jobRunner;
        this.retentionDays = retentionDays;
        this.batchSize = batchSize;
    }

    @Scheduled(cron = "${app.audit.retention-cron:0 0 3 * * *}")
    @SchedulerLock(name = "AuditRetentionJob", lockAtMostFor = "15m", lockAtLeastFor = "1m")
    public void run() {
        jobRunner.runJob("audit_retention", this::deleteExpiredAuditLogs);
    }

    @Transactional
    public long deleteExpiredAuditLogs() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        long totalDeleted = 0;
        List<Long> batchIds;

        do {
            batchIds = auditLogRepo.findIdsOlderThan(cutoff, PageRequest.of(0, batchSize));
            if (!batchIds.isEmpty()) {
                int count = auditLogRepo.deleteByIdIn(batchIds);
                totalDeleted += count;
            }
        } while (!batchIds.isEmpty());

        log.info("Audit log retention completed: pruned {} rows older than {} days (cutoff: {})",
                totalDeleted, retentionDays, cutoff);
        return totalDeleted;
    }
}

