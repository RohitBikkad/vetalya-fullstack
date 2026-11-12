package com.pet.vetalya.health;

import com.pet.vetalya.repository.ReminderRepository;
import com.pet.vetalya.scheduling.ReminderScheduledJob;
import com.pet.vetalya.scheduling.ReusableJobRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Health check verifying that the reminder scheduling background worker is actively running
 * and reporting the total count of overdue unsent reminders.
 */
@Component("reminderScheduler")
public class ReminderSchedulerHealthIndicator extends CachedHealthIndicator {

    private final ReusableJobRunner jobRunner;
    private final ReminderRepository reminderRepo;
    private final Duration maxExpectedInterval;

    public ReminderSchedulerHealthIndicator(
            ReusableJobRunner jobRunner,
            ReminderRepository reminderRepo,
            @Value("${app.reminder.max-stale-interval-seconds:180}") long maxIntervalSec) {
        super(Duration.ofSeconds(15));
        this.jobRunner = jobRunner;
        this.reminderRepo = reminderRepo;
        this.maxExpectedInterval = Duration.ofSeconds(maxIntervalSec);
    }

    @Override
    protected Health doCheck() {
        ReusableJobRunner.JobState state = jobRunner.getJobState(ReminderScheduledJob.JOB_NAME);
        long overdueCount = reminderRepo.countOverdueReminders(Instant.now());

        boolean isHealthy = jobRunner.isJobHealthy(ReminderScheduledJob.JOB_NAME, maxExpectedInterval);

        Health.Builder builder = isHealthy ? Health.up() : Health.down();
        builder.withDetail("overdueRemindersCount", overdueCount);

        if (state != null) {
            builder.withDetail("lastStartTime", state.lastStartTime() != null ? state.lastStartTime().toString() : "none")
                    .withDetail("lastSuccessTime", state.lastSuccessTime() != null ? state.lastSuccessTime().toString() : "none")
                    .withDetail("lastProcessedCount", state.lastProcessedCount())
                    .withDetail("consecutiveFailures", state.totalFailures());
        } else {
            builder.withDetail("status", "Job waiting for first execution");
        }

        if (!isHealthy) {
            builder.withDetail("error", "Reminder scheduler is stale or failing: exceeded "
                    + maxExpectedInterval.toSeconds() + "s without success");
        }

        return builder.build();
    }
}
