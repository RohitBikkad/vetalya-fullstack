package com.pet.vetalya.auth.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:${spring.mail.username:}}")
    private String fromEmail;

    @Value("${spring.mail.host:}")
    private String mailHost;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Dispatches a 6-digit OTP verification email.
     * Uses JavaMailSender / SMTP if configured, with graceful fallback to logger.
     */
    @Async
    public void sendOtpEmail(String toEmail, String otpCode, String purpose) {
        if (toEmail == null || !toEmail.contains("@")) {
            return;
        }

        String sender = (fromEmail != null && !fromEmail.isBlank()) ? fromEmail : "noreply@vetalya.com";
        String cleanPurpose = (purpose != null) ? purpose.replace("_", " ").toUpperCase() : "VERIFICATION";

        if (mailSender != null && mailHost != null && !mailHost.isBlank()) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(sender, "Vetalya Healthcare Platform");
                helper.setTo(toEmail);
                helper.setSubject("[Vetalya] " + otpCode + " is your verification code");

                String htmlContent = """
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

                helper.setText(htmlContent, true);
                mailSender.send(message);
                log.info("✅ [EmailService] Verification email successfully dispatched to [{}]", toEmail);
                return;
            } catch (Exception ex) {
                log.warn("⚠️ [EmailService] SMTP delivery to [{}] encountered an issue: {}. Falling back to gateway log.", toEmail, ex.getMessage());
            }
        }

        // Fallback / simulated delivery log
        log.info("📲 [EmailService Simulated] OTP Code [{}] for [{}] (Purpose: {})", otpCode, toEmail, cleanPurpose);
    }
}

