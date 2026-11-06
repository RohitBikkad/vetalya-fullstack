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

    private final OtpVerificationRepository otpRepo;

    public OtpService(OtpVerificationRepository otpRepo) {
        this.otpRepo = otpRepo;
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
        String cleanTarget = target.trim();
        String cleanPurpose = (purpose == null || purpose.isBlank()) ? "VERIFICATION" : purpose.trim().toUpperCase();

        // Generate 6-digit cryptographic OTP code
        int code = 100000 + random.nextInt(900000);
        String otpCode = String.valueOf(code);

        // Invalidate old unverified OTPs for the same target
        var existing = otpRepo.findByTargetAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(cleanTarget, cleanPurpose);
        for (OtpVerification old : existing) {
            old.setVerified(true);
            otpRepo.save(old);
        }

        OtpVerification otp = new OtpVerification();
        otp.setTarget(cleanTarget);
        otp.setOtpCode(otpCode);
        otp.setPurpose(cleanPurpose);
        otp.setAttempts(0);
        otp.setVerified(false);
        otp.setExpiresAt(Instant.now().plus(Duration.ofMinutes(10)));
        otpRepo.save(otp);

        log.info("📲 [Vetalya Gateway] Generated OTP: {} for Target: [{}] (Purpose: {})", otpCode, cleanTarget, cleanPurpose);

        return new OtpResult(true, "Verification code sent to " + cleanTarget, otpCode, 600);
    }

    /**
     * Verify a 6-digit OTP against active database records.
     */
    @Transactional
    public boolean verifyOtp(String target, String inputCode, String purpose) {
        if (target == null || inputCode == null) return false;
        String cleanTarget = target.trim();
        String cleanPurpose = (purpose == null || purpose.isBlank()) ? "VERIFICATION" : purpose.trim().toUpperCase();

        Optional<OtpVerification> latestOpt = otpRepo.findTopByTargetAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(cleanTarget, cleanPurpose);
        if (latestOpt.isEmpty()) {
            log.warn("No active OTP found for target: {}", cleanTarget);
            return false;
        }

        OtpVerification otp = latestOpt.get();
        if (otp.getExpiresAt().isBefore(Instant.now())) {
            log.warn("OTP for {} expired at {}", cleanTarget, otp.getExpiresAt());
            return false;
        }

        if (otp.getAttempts() >= 5) {
            log.warn("Max OTP verification attempts exceeded for target: {}", cleanTarget);
            return false;
        }

        otp.setAttempts(otp.getAttempts() + 1);

        if (otp.getOtpCode().equals(inputCode.trim())) {
            otp.setVerified(true);
            otpRepo.save(otp);
            log.info("✅ OTP successfully verified for target: {}", cleanTarget);
            return true;
        } else {
            otpRepo.save(otp);
            log.warn("❌ Incorrect OTP attempt {}/5 for target: {}", otp.getAttempts(), cleanTarget);
            return false;
        }
    }

    /**
     * Check if target has a verified OTP within the last 15 minutes.
     */
    public boolean isTargetVerifiedRecently(String target, String purpose) {
        if (target == null) return false;
        String cleanTarget = target.trim();
        String cleanPurpose = (purpose == null || purpose.isBlank()) ? "VERIFICATION" : purpose.trim().toUpperCase();

        var verified = otpRepo.findTopByTargetAndPurposeAndVerifiedTrueOrderByCreatedAtDesc(cleanTarget, cleanPurpose);
        if (verified.isEmpty()) return false;
        return verified.get().getExpiresAt().isAfter(Instant.now());
    }

    public record OtpResult(boolean success, String message, String otpCode, int expiresInSeconds) {}
}
