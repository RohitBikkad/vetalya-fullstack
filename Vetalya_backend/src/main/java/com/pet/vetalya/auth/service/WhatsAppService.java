package com.pet.vetalya.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);
    private static final String META_GRAPH_BASE_URL = "https://graph.facebook.com/v21.0";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${app.whatsapp.access-token:${WHATSAPP_ACCESS_TOKEN:}}")
    private String accessToken;

    @Value("${app.whatsapp.phone-number-id:${WHATSAPP_PHONE_NUMBER_ID:1500405019813304}}")
    private String phoneNumberId;

    @Value("${app.whatsapp.enabled:true}")
    private boolean enabled;

    public WhatsAppService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Dispatches a 6-digit OTP verification code via Meta WhatsApp Cloud API over HTTPS port 443.
     */
    @Async
    public void sendOtpWhatsApp(String rawPhone, String otpCode) {
        if (!enabled) {
            log.info("📲 [WhatsApp API] Disabled. OTP code [{}] simulated for [{}]", otpCode, rawPhone);
            return;
        }

        if (accessToken == null || accessToken.isBlank() || phoneNumberId == null || phoneNumberId.isBlank()) {
            log.warn("⚠️ [WhatsApp API] Access token or Phone Number ID is missing. Skipping WhatsApp dispatch for {}", rawPhone);
            return;
        }

        String formattedPhone = formatToInternational(rawPhone);
        if (formattedPhone == null || formattedPhone.length() < 10) {
            log.warn("⚠️ [WhatsApp API] Invalid phone number [{}]", rawPhone);
            return;
        }

        try {
            String url = META_GRAPH_BASE_URL + "/" + phoneNumberId.trim() + "/messages";

            String messageText = "🐾 *[Vetalya]* Your verification code is: *" + otpCode.trim()
                    + "*\n\nValid for 10 minutes. Please do not share this code with anyone.";

            Map<String, Object> payload = Map.of(
                    "messaging_product", "whatsapp",
                    "recipient_type", "individual",
                    "to", formattedPhone,
                    "type", "text",
                    "text", Map.of(
                            "preview_url", false,
                            "body", messageText
                    )
            );

            String jsonBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken.trim())
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(8))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                log.info("✅ [WhatsApp Cloud API] OTP successfully delivered to [{}xxxxxx{}]",
                        formattedPhone.substring(0, Math.min(4, formattedPhone.length())),
                        formattedPhone.substring(Math.max(0, formattedPhone.length() - 2)));
            } else {
                log.warn("⚠️ [WhatsApp Cloud API Error] HTTP {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception ex) {
            log.warn("⚠️ [WhatsApp Cloud API Exception] Failed to send WhatsApp to {}: {}", rawPhone, ex.getMessage());
        }
    }

    /**
     * Formats phone number into international E.164 format without '+' (e.g. 919921128469 for India).
     */
    private String formatToInternational(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() == 10) {
            // Standard Indian 10-digit number -> prepend country code 91
            return "91" + digits;
        }
        if (digits.length() == 11 && digits.startsWith("0")) {
            return "91" + digits.substring(1);
        }
        return digits;
    }
}

