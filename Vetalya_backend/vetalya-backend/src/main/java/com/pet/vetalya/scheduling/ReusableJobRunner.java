package com.pet.vetalya.scheduling;

import com.pet.vetalya.logging.CorrelationIdFilter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Reusable monitoring wrapper for scheduled background jobs.
 * Enforces per-execution jobRunIds in MDC, records start/success/duration metrics,
 * protects the scheduling loop against uncaught runtime failures, and exposes execution health.
 */
@Component
public class ReusableJobRunner {

    private static final Logger log = LoggerFactory.getLogger(ReusableJobRunner.class);

    private final MeterRegistry meterRegistry;
    private final Map<String, JobState> jobStates = new ConcurrentHashMap<>();

    public interface JobTask {
        long run() throws Exception;
    }

    public record JobState(
            Instant lastStartTime,
            Instant lastSuccessTime,
            Instant lastFailureTime,
            long lastProcessedCount,
            long totalFailures
    ) {}

    public ReusableJobRunner(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void runJob(String jobName, JobTask task) {
        String jobRunId = UUID.randomUUID().toString();
        MDC.put(CorrelationIdFilter.MDC_CORRELATION_ID_KEY, jobRunId);
        MDC.put(CorrelationIdFilter.MDC_TRACE_ID_KEY, jobRunId);
        MDC.put("jobRunId", jobRunId);

        Instant start = Instant.now();
        log.info("Job [{}] started. jobRunId={}", jobName, jobRunId);

        Timer timer = Timer.builder("jobs.duration")
                .tag("job", jobName)
                .register(meterRegistry);

        long processed = 0;
        boolean success = false;
        long durationMs = 0;

        try {
            long startTimeNanos = System.nanoTime();
            processed = task.run();
            durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);
            timer.record(durationMs, TimeUnit.MILLISECONDS);

            Counter.builder("jobs.processed")
                    .tag("job", jobName)
                    .register(meterRegistry)
                    .increment(processed);

            success = true;
            log.info("Job [{}] finished successfully. jobRunId={} duration={}ms processed={}",
                    jobName, jobRunId, durationMs, processed);
        } catch (Throwable t) {
            durationMs = Duration.between(start, Instant.now()).toMillis();
            Counter.builder("jobs.failed")
                    .tag("job", jobName)
                    .register(meterRegistry)
                    .increment();

            log.error("Job [{}] failed with error. jobRunId={} duration={}ms: {}",
                    jobName, jobRunId, durationMs, t.getMessage(), t);
        } finally {
            Instant finish = Instant.now();
            updateJobState(jobName, start, success ? finish : null, !success ? finish : null, processed, !success);
            MDC.remove(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
            MDC.remove(CorrelationIdFilter.MDC_TRACE_ID_KEY);
            MDC.remove("jobRunId");
        }
    }

    public JobState getJobState(String jobName) {
        return jobStates.get(jobName);
    }

    public boolean isJobHealthy(String jobName, Duration maxExpectedInterval) {
        JobState state = jobStates.get(jobName);
        if (state == null) {
            return true; // Not run yet, assumed initializing
        }
        if (state.lastSuccessTime() == null) {
            return false;
        }
        return Duration.between(state.lastSuccessTime(), Instant.now()).compareTo(maxExpectedInterval) <= 0;
    }

    private void updateJobState(String jobName, Instant start, Instant successTime, Instant failureTime,
                                long processed, boolean failed) {
        jobStates.compute(jobName, (k, old) -> {
            long totalFailures = (old != null ? old.totalFailures() : 0) + (failed ? 1 : 0);
            Instant lastSuccess = successTime != null ? successTime : (old != null ? old.lastSuccessTime() : null);
            Instant lastFailure = failureTime != null ? failureTime : (old != null ? old.lastFailureTime() : null);
            return new JobState(start, lastSuccess, lastFailure, processed, totalFailures);
        });
    }
}

