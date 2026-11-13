package com.pet.vetalya.auth.service;

import com.pet.vetalya.entity.OtpVerification;
import com.pet.vetalya.repository.OtpVerificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom random = new SecureRandom();
    private final org.springframework.security.crypto.password.PasswordEncoder codeEncoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    private static String target(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Email or phone is required");
        return value.contains("@") ? value.trim().toLowerCase(java.util.Locale.ROOT) : value.trim();
    }
    private static String purpose(String value) {
        String result = value == null || value.isBlank() ? "VERIFICATION" : value.trim().toUpperCase(java.util.Locale.ROOT);
        if (!java.util.Set.of("REGISTRATION", "VERIFICATION", "LOGIN").contains(result)) throw new IllegalArgumentException("Invalid verification purpose");
        return result;
    }
    private static String hash(String value) {
        try { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private final OtpVerificationRepository otpRepo;
    private final EmailService emailService;
    private final Fast2SmsService fast2SmsService;
    private final WhatsAppService whatsAppService;

    public OtpService(OtpVerificationRepository otpRepo,
                      EmailService emailService,
                      Fast2SmsService fast2SmsService,
                      WhatsAppService whatsAppService) {
        this.otpRepo = otpRepo;
        this.emailService = emailService;
        this.fast2SmsService = fast2SmsService;
        this.whatsAppService = whatsAppService;
    }

    /**
     * Generate and dispatch a 6-digit OTP for SMS / Email.
     * In simulated / developer mode, logs the OTP clearly and returns it in result.
     */
    @Transactional
    public OtpResult sendOtp(String target, String purpose) {
        if (target == null || target.isBlank()) {
            throw new IllegalArgumentException("Mobile phone number or email target is required");
        }
        String cleanTarget = target(target);
        String cleanPurpose = purpose(purpose);

        if (cleanTarget.contains("@") && !cleanTarget.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw new IllegalArgumentException("Enter a valid email address");
        var previous = otpRepo.findTopByTargetAndPurposeOrderByCreatedAtDesc(cleanTarget, cleanPurpose);
        if (previous.isPresent() && previous.get().getCreatedAt().isAfter(Instant.now().minusSeconds(60)))
            throw new IllegalArgumentException("Wait 60 seconds before requesting another code");

        // Generate 6-digit cryptographic OTP code
        int code = 100000 + random.nextInt(900000);
        String otpCode = String.valueOf(code);

        // Invalidate old unverified OTPs for the same target
        var existing = otpRepo.findByTargetAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(cleanTarget, cleanPurpose);
        for (OtpVerification old : existing) {
            old.setExpiresAt(Instant.now().minusSeconds(10));
            old.setAttempts(5);
            otpRepo.save(old);
        }

        OtpVerification otp = new OtpVerification();
        otp.setTarget(cleanTarget);
        otp.setOtpCode(codeEncoder.encode(otpCode));
        otp.setPurpose(cleanPurpose);
        otp.setAttempts(0);
        otp.setVerified(false);
        otp.setExpiresAt(Instant.now().plus(Duration.ofMinutes(10)));
        otpRepo.save(otp);



        // If target is an email address, dispatch email; otherwise dispatch WhatsApp & SMS
        if (cleanTarget.contains("@")) {
            emailService.sendOtpEmail(cleanTarget, otpCode, cleanPurpose);
        } else {
            whatsAppService.sendOtpWhatsApp(cleanTarget, otpCode);
            fast2SmsService.sendOtpSms(cleanTarget, otpCode);
        }

        return new OtpResult(true, "Verification code sent to " + cleanTarget, null, 600);
    }

    /**
     * Verify a 6-digit OTP against active database records.
     */
    @Transactional
    public boolean verifyOtp(String target, String inputCode, String purpose) {
        return verify(target, inputCode, purpose, false) != null;
    }

    @Transactional
    public String verifyEmailRegistration(String email, String code) {
        return verify(email, code, "REGISTRATION", true);
    }

    private String verify(String rawTarget, String code, String rawPurpose, boolean receipt) {
        if (rawTarget == null || code == null || !code.matches("[0-9]{6}")) return null;
        var latest = otpRepo.findTopByTargetAndPurposeOrderByCreatedAtDesc(target(rawTarget), purpose(rawPurpose));
        if (latest.isEmpty()) return null;
        var otp = latest.get();
        if (otp.isVerified() || !otp.getExpiresAt().isAfter(Instant.now()) || otp.getAttempts() >= 5) return null;
        otp.setAttempts(otp.getAttempts() + 1);
        if (!codeEncoder.matches(code, otp.getOtpCode())) { otpRepo.save(otp); return null; }
        otp.setVerified(true);
        String token = receipt ? java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(random.generateSeed(32)) : "verified";
        if (receipt) otp.setVerificationTokenHash(hash(token));
        otpRepo.save(otp);
        return token;
    }

    @Transactional
    public boolean consumeEmailVerification(String email, String token) {
        if (email == null || token == null || token.length() < 32 || token.length() > 128) return false;
        var latest = otpRepo.findTopByTargetAndPurposeOrderByCreatedAtDesc(target(email), "REGISTRATION");
        if (latest.isEmpty()) return false;
        var otp = latest.get();
        if (!otp.isVerified() || !otp.getExpiresAt().isAfter(Instant.now()) || otp.getVerificationTokenHash() == null
            || !java.security.MessageDigest.isEqual(hash(token).getBytes(java.nio.charset.StandardCharsets.US_ASCII), otp.getVerificationTokenHash().getBytes(java.nio.charset.StandardCharsets.US_ASCII))) return false;
        otp.setVerificationTokenHash(null);
        otp.setExpiresAt(Instant.now());
        otpRepo.save(otp);
        return true;
    }

    /**
     * Check if target has a verified OTP within the last 15 minutes.
     */
    public boolean isTargetVerifiedRecently(String target, String purpose) {
        if (target == null || target.isBlank()) return false;
        String cleanTarget = target(target);
        String cleanPurpose = (purpose == null || purpose.isBlank()) ? "REGISTRATION" : purpose.trim().toUpperCase();

        var verified = otpRepo.findTopByTargetAndPurposeAndVerifiedTrueOrderByCreatedAtDesc(cleanTarget, cleanPurpose);
        if (verified.isPresent() && verified.get().getCreatedAt().isAfter(Instant.now().minus(Duration.ofMinutes(15)))) {
            return true;
        }
        if (!"VERIFICATION".equals(cleanPurpose)) {
            var fallback = otpRepo.findTopByTargetAndPurposeAndVerifiedTrueOrderByCreatedAtDesc(cleanTarget, "VERIFICATION");
            return fallback.isPresent() && fallback.get().getCreatedAt().isAfter(Instant.now().minus(Duration.ofMinutes(15)));
        }
        return false;
    }

    public record OtpResult(boolean success, String message, String otpCode, int expiresInSeconds) {}
}
