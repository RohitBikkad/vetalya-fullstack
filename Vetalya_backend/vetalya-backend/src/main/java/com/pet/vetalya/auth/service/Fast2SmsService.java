package com.pet.vetalya.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class Fast2SmsService {

    private static final Logger log = LoggerFactory.getLogger(Fast2SmsService.class);
    private static final String FAST2SMS_URL = "https://www.fast2sms.com/dev/bulkV2";

    private final HttpClient httpClient;

    @Value("${app.fast2sms.api-key:5FWBTq1UHXEVQklkkBsMPSNRpNXgiJJYhifxeUrqriydM9YFMOx1JTHWwTrq7kA2mPgEUPoqLFkpcIbQ}")
    private String apiKey;

    @Value("${app.fast2sms.enabled:true}")
    private boolean enabled;

    public Fast2SmsService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Dispatch a 6-digit OTP to an Indian mobile phone number using Fast2SMS Quick OTP API.
     */
    @Async
    public void sendOtpSms(String rawPhone, String otpCode) {
        if (!enabled) {
            log.info("📲 [Fast2SMS] Disabled. OTP code [{}] simulated for [{}]", otpCode, rawPhone);
            return;
        }

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("⚠️ [Fast2SMS] API key is missing. Skipping SMS delivery for {}", rawPhone);
            return;
        }

        String cleanedPhone = sanitizePhoneNumber(rawPhone);
        if (cleanedPhone == null || cleanedPhone.length() != 10) {
            log.warn("⚠️ [Fast2SMS] Phone number [{}] is not a valid 10-digit Indian mobile number", rawPhone);
            return;
        }

        try {
            String requestUrl = FAST2SMS_URL
                    + "?authorization=" + URLEncoder.encode(apiKey.trim(), StandardCharsets.UTF_8)
                    + "&route=otp"
                    + "&variables_values=" + URLEncoder.encode(otpCode.trim(), StandardCharsets.UTF_8)
                    + "&numbers=" + URLEncoder.encode(cleanedPhone, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .header("cache-control", "no-cache")
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body();
                if (body != null && body.contains("\"status_code\":996")) {
                    log.warn("⚠️ [Fast2SMS Status 996] Account website verification required in Fast2SMS dashboard: {}", body);
                } else if (body != null && body.contains("\"return\":true")) {
                    log.info("✅ [Fast2SMS] OTP SMS successfully delivered to [{}xxxxxx{}]",
                            cleanedPhone.substring(0, 2), cleanedPhone.substring(8));
                } else {
                    log.info("📲 [Fast2SMS Response] Status: {} - Body: {}", response.statusCode(), body);
                }
            } else {
                log.warn("⚠️ [Fast2SMS Error] HTTP {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception ex) {
            log.warn("⚠️ [Fast2SMS Exception] Failed to send SMS to {}: {}", rawPhone, ex.getMessage());
        }
    }

    /**
     * Strips leading +91, 91, 0, spaces, and hyphens to yield clean 10 digits.
     */
    private String sanitizePhoneNumber(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() == 12 && digits.startsWith("91")) {
            return digits.substring(2);
        }
        if (digits.length() == 11 && digits.startsWith("0")) {
            return digits.substring(1);
        }
        return digits;
    }
}
