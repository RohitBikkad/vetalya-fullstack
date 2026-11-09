package com.pet.vetalya.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Centralized business metrics service.
 * Enforces strict tag low-cardinality (never accepts user IDs, raw URLs, or phone numbers as metric tags).
 */
@Component
public class AppMetrics {

    private final MeterRegistry registry;

    // Gauges
    private final AtomicLong overdueRemindersCount = new AtomicLong(0);
    private final AtomicInteger unverifiedVetsCount = new AtomicInteger(0);

    // Cheap 15-minute active users approximation using two rolling 15-minute time buckets
    private final Set<String> currentActiveUsersBucket = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<String> previousActiveUsersBucket = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private volatile long currentBucketEpochMinute = Instant.now().getEpochSecond() / 60;

    public AppMetrics(MeterRegistry registry) {
        this.registry = registry;

        // Register custom gauges
        registry.gauge("reminders.overdue", overdueRemindersCount);
        registry.gauge("vets.unverified.pending", unverifiedVetsCount);
        registry.gauge("users.active.15min", this, AppMetrics::estimateActiveUsers);
    }

    // ── Authentication & Security Metrics ──

    public void recordLogin(String provider, String outcome) {
        Counter.builder("auth.login")
                .tag("provider", sanitizeTag(provider, "unknown"))
                .tag("outcome", sanitizeTag(outcome, "failure"))
                .register(registry)
                .increment();
    }

    public void recordOtpSent(String outcome) {
        Counter.builder("auth.otp.sent")
                .tag("outcome", sanitizeTag(outcome, "failure"))
                .register(registry)
                .increment();
    }

    public void recordOtpVerify(String outcome) {
        Counter.builder("auth.otp.verify")
                .tag("outcome", sanitizeTag(outcome, "failure"))
                .register(registry)
                .increment();
    }

    public void recordTokenRefresh(String outcome) {
        Counter.builder("auth.refresh")
                .tag("outcome", sanitizeTag(outcome, "failure"))
                .register(registry)
                .increment();
    }

    public void recordTokenReuseDetected() {
        Counter.builder("auth.refresh.reuse_detected")
                .register(registry)
                .increment();
    }

    public void recordBruteForceSignal(String type) {
        Counter.builder("auth.bruteforce.signals")
                .tag("type", sanitizeTag(type, "ip"))
                .register(registry)
                .increment();
    }

    // ── Appointment & Business Metrics ──

    public void recordAppointmentBooked() {
        Counter.builder("appointments.booked").register(registry).increment();
    }

    public void recordAppointmentCancelled() {
        Counter.builder("appointments.cancelled").register(registry).increment();
    }

    public void recordAppointmentStatusChange(String from, String to) {
        Counter.builder("appointments.status_change")
                .tag("from", sanitizeTag(from, "UNKNOWN"))
                .tag("to", sanitizeTag(to, "UNKNOWN"))
                .register(registry)
                .increment();
    }

    public void recordAppointmentSlotConflict() {
        Counter.builder("appointment.slot_conflict").register(registry).increment();
    }

    public void recordReminderSent() {
        Counter.builder("reminders.sent").register(registry).increment();
    }

    public void recordReminderFailed() {
        Counter.builder("reminders.failed").register(registry).increment();
    }

    public void setOverdueReminders(long count) {
        this.overdueRemindersCount.set(count);
    }

    public void setUnverifiedVetsCount(int count) {
        this.unverifiedVetsCount.set(count);
    }

    public void recordReviewCreated() {
        Counter.builder("reviews.created").register(registry).increment();
    }

    public void recordVetSearch(Duration duration) {
        Timer.builder("vets.search")
                .register(registry)
                .record(duration);
    }

    public void recordExternalCall(String dependency, String outcome, Duration duration) {
        Timer.builder("external.call")
                .tag("dependency", sanitizeTag(dependency, "external"))
                .tag("outcome", sanitizeTag(outcome, "unknown"))
                .register(registry)
                .record(duration);
    }

    public <T> T recordExternalCall(String dependency, Supplier<T> supplier) {
        long start = System.nanoTime();
        String outcome = "success";
        try {
            return supplier.get();
        } catch (RuntimeException e) {
            outcome = "failure";
            throw e;
        } finally {
            Duration duration = Duration.ofNanos(System.nanoTime() - start);
            recordExternalCall(dependency, outcome, duration);
        }
    }

    public void recordAuditWriteFailed(String reason) {
        Counter.builder("audit.write.failed")
                .tag("reason", sanitizeTag(reason, "unknown"))
                .register(registry)
                .increment();
    }

    // ── Active Users Approximation ──

    public void markUserActive(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        long nowMinute = Instant.now().getEpochSecond() / 60;
        if (nowMinute - currentBucketEpochMinute >= 15) {
            synchronized (this) {
                if (nowMinute - currentBucketEpochMinute >= 15) {
                    previousActiveUsersBucket.clear();
                    previousActiveUsersBucket.addAll(currentActiveUsersBucket);
                    currentActiveUsersBucket.clear();
                    currentBucketEpochMinute = nowMinute;
                }
            }
        }
        currentActiveUsersBucket.add(userId);
    }

    private double estimateActiveUsers() {
        Set<String> combined = ConcurrentHashMap.newKeySet();
        combined.addAll(currentActiveUsersBucket);
        combined.addAll(previousActiveUsersBucket);
        return combined.size();
    }

    private String sanitizeTag(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim().toLowerCase();
    }
}

