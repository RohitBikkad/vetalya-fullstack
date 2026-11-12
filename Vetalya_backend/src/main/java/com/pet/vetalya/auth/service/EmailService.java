package com.pet.vetalya.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final JavaMailSender mailSender;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${app.brevo.api-key:${BREVO_API_KEY:}}")
    private String brevoApiKey;

    @Value("${spring.mail.from:${spring.mail.username:}}")
    private String fromEmail;

    @Value("${spring.mail.host:}")
    private String mailHost;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Dispatches a 6-digit OTP verification email.
     * Prioritizes Brevo HTTPS REST API (port 443 - works on Render Free/Starter where SMTP port 587 is blocked).
     * Falls back to JavaMailSender SMTP, then simulated logger.
     */
    @Async
    public void sendOtpEmail(String toEmail, String otpCode, String purpose) {
        if (toEmail == null || !toEmail.contains("@")) {
            return;
        }

        String sender = (fromEmail != null && !fromEmail.isBlank()) ? fromEmail : "noreply@vetalya.com";
        String cleanPurpose = (purpose != null) ? purpose.replace("_", " ").toUpperCase() : "VERIFICATION";
        String htmlContent = buildOtpHtmlContent(cleanPurpose, otpCode);

        // ── 1. Priority: Brevo REST API over HTTPS (Bypasses Render SMTP port 587 blocks) ──
        if (brevoApiKey != null && !brevoApiKey.isBlank()) {
            try {
                Map<String, Object> payload = Map.of(
                        "sender", Map.of("name", "Vetalya Healthcare Platform", "email", sender),
                        "to", List.of(Map.of("email", toEmail)),
                        "subject", "[Vetalya] " + otpCode + " is your verification code",
                        "htmlContent", htmlContent
                );

                String jsonBody = objectMapper.writeValueAsString(payload);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(BREVO_API_URL))
                        .header("accept", "application/json")
                        .header("content-type", "application/json")
                        .header("api-key", brevoApiKey.trim())
                        .timeout(Duration.ofSeconds(8))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 201 || response.statusCode() == 200) {
                    log.info("✅ [Brevo API HTTP 443] Verification email successfully delivered to [{}]", toEmail);
                    return;
                } else {
                    log.warn("⚠️ [Brevo API Error] HTTP {}: {}", response.statusCode(), response.body());
                }
            } catch (Exception ex) {
                log.warn("⚠️ [Brevo API Exception] Failed to send email via Brevo REST API to [{}]: {}", toEmail, ex.getMessage());
            }
        }

        // ── 2. Fallback: Standard SMTP via JavaMailSender (if not blocked by host firewall) ──
        if (mailSender != null && mailHost != null && !mailHost.isBlank()) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(sender, "Vetalya Healthcare Platform");
                helper.setTo(toEmail);
                helper.setSubject("[Vetalya] " + otpCode + " is your verification code");
                helper.setText(htmlContent, true);

                mailSender.send(message);
                log.info("✅ [EmailService SMTP] Verification email successfully dispatched to [{}]", toEmail);
                return;
            } catch (Exception ex) {
                log.warn("⚠️ [EmailService SMTP] Delivery to [{}] failed: {}. (Render Free/Starter blocks SMTP port 587)", toEmail, ex.getMessage());
            }
        }

        // ── 3. Final Fallback: Simulated Delivery ──
        log.info("📲 [EmailService Simulated] OTP Code [{}] for [{}] (Purpose: {})", otpCode, toEmail, cleanPurpose);
    }

    private String buildOtpHtmlContent(String cleanPurpose, String otpCode) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #F7F5EE; margin: 0; padding: 24px; color: #242826; }
                    .card { max-width: 480px; margin: 0 auto; background: #ffffff; border-radius: 24px; padding: 36px 32px; border: 1px solid rgba(16, 50, 78, 0.08); box-shadow: 0 10px 25px rgba(0,0,0,0.05); }
                    .brand { display: flex; align-items: center; gap: 8px; font-size: 20px; font-weight: 800; color: #10324E; margin-bottom: 24px; }
                    .brand-heart { color: #F49E78; }
                    .title { font-size: 20px; font-weight: 800; color: #10324E; margin-bottom: 8px; }
                    .text { font-size: 14px; line-height: 1.6; color: #4A4D4B; margin-bottom: 24px; }
                    .code-box { background: #F5F2EB; border-radius: 16px; padding: 18px 24px; text-align: center; margin-bottom: 24px; border: 1px solid rgba(16, 50, 78, 0.08); }
                    .code { font-family: 'Courier New', Courier, monospace; font-size: 32px; font-weight: 800; letter-spacing: 8px; color: #10324E; }
                    .badge { display: inline-block; padding: 4px 10px; background: #E0F2EB; color: #2D8A7A; border-radius: 8px; font-size: 11px; font-weight: 700; text-transform: uppercase; margin-bottom: 12px; }
                    .footer { font-size: 12px; color: #8F918F; text-align: center; margin-top: 24px; line-height: 1.5; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="brand">
                        <span class="brand-heart">♥</span> Vetalya
                    </div>
                    <span class="badge">%s</span>
                    <div class="title">Verify your email address</div>
                    <p class="text">
                        Please use the 6-digit verification code below to complete your %s on the Vetalya Healthcare Platform:
                    </p>
                    <div class="code-box">
                        <div class="code">%s</div>
                    </div>
                    <p class="text" style="font-size: 12px; color: #737573;">
                        ⏱️ This code expires in <strong>10 minutes</strong>. If you did not initiate this request, you can safely ignore this email.
                    </p>
                    <div class="footer">
                        © 2026 Vetalya • Animal Healthcare & Veterinary Platform<br>
                        Every Pet. Health. Always.
                    </div>
                </div>
            </body>
            </html>
            """.formatted(cleanPurpose, cleanPurpose.toLowerCase(), otpCode);
    }
}
