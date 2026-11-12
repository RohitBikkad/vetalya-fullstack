package com.pet.vetalya.metrics;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MetricsCardinalityTest {

    @Test
    @DisplayName("Metric tags contain no high-cardinality data like user IDs, emails, or tokens")
    void metricTagsContainNoHighCardinalityData() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AppMetrics metrics = new AppMetrics(registry);

        // Record various business operations
        metrics.recordLogin("google", "success");
        metrics.recordLogin("apple", "failure");
        metrics.recordOtpSent("success");
        metrics.recordOtpVerify("failure");
        metrics.recordTokenRefresh("success");
        metrics.recordAppointmentStatusChange("BOOKED", "CONFIRMED");
        metrics.recordExternalCall("twilio_sms", "success", Duration.ofMillis(120));

        // Scan all meters and inspect tag keys and values
        List<Meter> meters = registry.getMeters();
        assertThat(meters).isNotEmpty();

        for (Meter meter : meters) {
            String meterName = meter.getId().getName();
            assertThat(meterName).isNotBlank();

            for (Tag tag : meter.getId().getTags()) {
                String key = tag.getKey();
                String value = tag.getValue();

                // 1. Tag keys must never represent dynamic identifiers
                assertThat(key).doesNotContain("userId", "user_id", "email", "phone", "token", "password");

                // 2. Tag values must be standardized low-cardinality codes
                assertThat(value).doesNotContain("@"); // No email
                assertThat(value).doesNotMatch("^\\+?\\d{10,}$"); // No raw phone numbers
                assertThat(value).doesNotContain("bearer"); // No bearer tokens
                assertThat(value).doesNotMatch("^[0-9a-fA-F-]{36}$"); // No raw UUIDs
            }
        }
    }

    @Test
    @DisplayName("BruteForceSignalService increments operational metric without throwing")
    void bruteForceSignalTrigger() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AppMetrics metrics = new AppMetrics(registry);
        BruteForceSignalService service = new BruteForceSignalService(metrics, 3);

        service.recordIpFailure("192.168.1.100");
        service.recordIpFailure("192.168.1.100");
        // 3rd failure reaches threshold of 3
        service.recordIpFailure("192.168.1.100");

        double signals = registry.counter("auth.bruteforce.signals", "type", "ip").count();
        assertThat(signals).isEqualTo(1.0);
    }
}

