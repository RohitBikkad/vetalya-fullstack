package com.pet.vetalya.metrics;

import com.pet.vetalya.logging.MaskingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory sliding-window tracker that identifies repeated authentication failures per IP and phone number.
 * When the failure threshold is crossed, an operational metric is counted and a WARN log is generated.
 * This service never blocks authentication requests directly.
 */
@Service
public class BruteForceSignalService {

    private static final Logger log = LoggerFactory.getLogger(BruteForceSignalService.class);

    private final AppMetrics appMetrics;
    private final int threshold;

    private final Map<String, WindowCounter> ipFailures = new ConcurrentHashMap<>();
    private final Map<String, WindowCounter> phoneFailures = new ConcurrentHashMap<>();

    public BruteForceSignalService(
            AppMetrics appMetrics,
            @Value("${app.security.brute-force-threshold:5}") int threshold) {
        this.appMetrics = appMetrics;
        this.threshold = threshold;
    }

    public void recordIpFailure(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return;
        }
        int count = ipFailures.computeIfAbsent(ipAddress, k -> new WindowCounter()).increment();
        if (count == threshold) {
            log.warn("Brute-force signal: IP {} crossed authentication failure threshold ({} failures in 15m)",
                    ipAddress, count);
            appMetrics.recordBruteForceSignal("ip");
        } else if (count > threshold && count % threshold == 0) {
            log.warn("Brute-force sustained: IP {} reached {} consecutive authentication failures",
                    ipAddress, count);
            appMetrics.recordBruteForceSignal("ip");
        }
    }

    public void recordPhoneFailure(String phone) {
        if (phone == null || phone.isBlank()) {
            return;
        }
        int count = phoneFailures.computeIfAbsent(phone, k -> new WindowCounter()).increment();
        if (count == threshold) {
            log.warn("Brute-force signal: Phone {} crossed OTP failure threshold ({} failures in 15m)",
                    MaskingUtil.maskPhone(phone), count);
            appMetrics.recordBruteForceSignal("phone");
        } else if (count > threshold && count % threshold == 0) {
            log.warn("Brute-force sustained: Phone {} reached {} consecutive OTP failures",
                    MaskingUtil.maskPhone(phone), count);
            appMetrics.recordBruteForceSignal("phone");
        }
    }

    public void resetPhone(String phone) {
        if (phone != null) {
            phoneFailures.remove(phone);
        }
    }

    private static class WindowCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStartTime = System.currentTimeMillis();

        public int increment() {
            long now = System.currentTimeMillis();
            if (now - windowStartTime > 15 * 60 * 1000L) { // 15-minute sliding window
                synchronized (this) {
                    if (now - windowStartTime > 15 * 60 * 1000L) {
                        count.set(0);
                        windowStartTime = now;
                    }
                }
            }
            return count.incrementAndGet();
        }
    }
}

