package com.pet.vetalya.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Duration;

/**
 * Health check evaluating available JVM heap memory and filesystem storage against configured thresholds.
 */
@Component("memoryAndDisk")
public class MemoryDiskHealthIndicator extends CachedHealthIndicator {

    private final double maxHeapThresholdPercent;
    private final long minDiskThresholdBytes;

    public MemoryDiskHealthIndicator(
            @Value("${app.health.max-heap-percent:85.0}") double maxHeapThresholdPercent,
            @Value("${app.health.min-disk-bytes:524288000}") long minDiskThresholdBytes) { // 500MB
        super(Duration.ofSeconds(15));
        this.maxHeapThresholdPercent = maxHeapThresholdPercent;
        this.minDiskThresholdBytes = minDiskThresholdBytes;
    }

    @Override
    protected Health doCheck() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double heapUsedPercent = ((double) usedMemory / maxMemory) * 100.0;

        File root = new File(".");
        long usableSpace = root.getUsableSpace();

        Health.Builder builder = Health.up();

        builder.withDetail("heapUsedPercent", String.format("%.2f%%", heapUsedPercent))
                .withDetail("heapMaxMb", maxMemory / (1024 * 1024))
                .withDetail("heapUsedMb", usedMemory / (1024 * 1024))
                .withDetail("diskUsableMb", usableSpace / (1024 * 1024));

        if (heapUsedPercent > maxHeapThresholdPercent) {
            builder = Health.down()
                    .withDetail("error", "JVM heap memory exceeded threshold: " + String.format("%.2f%%", heapUsedPercent))
                    .withDetail("thresholdPercent", maxHeapThresholdPercent);
        }

        if (usableSpace < minDiskThresholdBytes) {
            builder = Health.down()
                    .withDetail("error", "Usable disk space below critical threshold: " + (usableSpace / (1024 * 1024)) + " MB")
                    .withDetail("thresholdMb", minDiskThresholdBytes / (1024 * 1024));
        }

        return builder.build();
    }
}
