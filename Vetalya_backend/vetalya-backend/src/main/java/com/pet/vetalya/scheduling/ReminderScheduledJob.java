package com.pet.vetalya.scheduling;

import com.pet.vetalya.entity.Reminder;
import com.pet.vetalya.metrics.AppMetrics;
import com.pet.vetalya.repository.ReminderRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Scheduled worker for sending appointment and care reminders.
 * Wrapped with ReusableJobRunner and ShedLock.
 * Each item is processed within its own error isolation boundary so a single failure does not stop the batch.
 */
@Component
public class ReminderScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduledJob.class);
    public static final String JOB_NAME = "reminder_sender";

    private final ReminderRepository reminderRepo;
    private final ReusableJobRunner jobRunner;
    private final AppMetrics appMetrics;

    public ReminderScheduledJob(
            ReminderRepository reminderRepo,
            ReusableJobRunner jobRunner,
            AppMetrics appMetrics) {
        this.reminderRepo = reminderRepo;
        this.jobRunner = jobRunner;
        this.appMetrics = appMetrics;
    }

    @Scheduled(fixedDelayString = "${app.reminder.interval-ms:60000}")
    @SchedulerLock(name = "ReminderScheduledJob", lockAtMostFor = "5m", lockAtLeastFor = "10s")
    public void run() {
        jobRunner.runJob(JOB_NAME, this::processDueReminders);
    }

    @Transactional
    public long processDueReminders() {
        Instant now = Instant.now();
        long overdueCount = reminderRepo.countOverdueReminders(now);
        appMetrics.setOverdueReminders(overdueCount);

        Page<Reminder> dueReminders = reminderRepo.findDueReminders(now, PageRequest.of(0, 100));
        long processed = 0;

        for (Reminder reminder : dueReminders.getContent()) {
            try {
                // Dispatch notification (SMS or Push)
                sendNotification(reminder);

                reminder.setSent(true);
                reminder.setSentAt(Instant.now());
                reminderRepo.save(reminder);

                appMetrics.recordReminderSent();
                log.info("reminder_sent: reminderId={}, appointmentId={}",
                        reminder.getReminderId(), reminder.getAppointmentId());
                processed++;
            } catch (Exception ex) {
                appMetrics.recordReminderFailed();
                log.warn("reminder_failed: reminderId={}, error={}",
                        reminder.getReminderId(), ex.getMessage());
            }
        }

        // Refresh overdue count after sending
        long remainingOverdue = reminderRepo.countOverdueReminders(Instant.now());
        appMetrics.setOverdueReminders(remainingOverdue);

        return processed;
    }

    private void sendNotification(Reminder reminder) {
        // External SMS / Push simulation
        log.debug("Dispatching reminder notification for appointment {}", reminder.getAppointmentId());
    }
}

