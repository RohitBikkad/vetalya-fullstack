package com.pet.vetalya.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Health indicator tracking SMS provider delivery success over a rolling window of recent attempts.
 * Avoids rate-limiting or balance check overhead by relying on passive attempt monitoring.
 */
@Component("smsProvider")
public class SmsProviderHealthIndicator extends CachedHealthIndicator {

    private final int windowSize;
    private final double minSuccessRatio;
    private final Deque<Boolean> recentAttempts = new ArrayDeque<>();

    public SmsProviderHealthIndicator(
            @Value("${app.sms.health.window-size:20}") int windowSize,
            @Value("${app.sms.health.min-success-ratio:0.8}") double minSuccessRatio) {
        super(Duration.ofSeconds(30));
        this.windowSize = windowSize;
        this.minSuccessRatio = minSuccessRatio;
    }

    public synchronized void recordAttempt(boolean success) {
        if (recentAttempts.size() >= windowSize) {
            recentAttempts.pollFirst();
        }
        recentAttempts.addLast(success);
        clearCache();
    }

    @Override
    protected synchronized Health doCheck() {
        if (recentAttempts.isEmpty()) {
            return Health.up()
                    .withDetail("provider", "SMS Gateway")
                    .withDetail("status", "Idle (No recent attempts)")
                    .build();
        }

        long successCount = recentAttempts.stream().filter(b -> Boolean.TRUE.equals(b)).count();
        double ratio = (double) successCount / recentAttempts.size();

        Health.Builder builder;
        if (recentAttempts.size() >= 5 && ratio < minSuccessRatio) {
            builder = Health.down()
                    .withDetail("error", "SMS delivery success ratio below threshold")
                    .withDetail("successRatio", String.format("%.2f", ratio))
                    .withDetail("sampleSize", recentAttempts.size())
                    .withDetail("threshold", minSuccessRatio);
        } else {
            builder = Health.up()
                    .withDetail("provider", "SMS Gateway")
                    .withDetail("successRatio", String.format("%.2f", ratio))
                    .withDetail("sampleSize", recentAttempts.size());
        }

        return builder.build();
    }
}
