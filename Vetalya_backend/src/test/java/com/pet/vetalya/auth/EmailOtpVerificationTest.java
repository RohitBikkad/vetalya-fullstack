package com.pet.vetalya.auth;

import com.pet.vetalya.auth.service.*;
import com.pet.vetalya.entity.OtpVerification;
import com.pet.vetalya.repository.OtpVerificationRepository;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class EmailOtpVerificationTest {
    OtpVerificationRepository repo;
    EmailService email;
    OtpService service;
    AtomicReference<OtpVerification> row;
    AtomicReference<String> deliveredCode;
    @BeforeEach void setup() {
        repo=mock(OtpVerificationRepository.class);email=mock(EmailService.class);
        service=new OtpService(repo,email,mock(Fast2SmsService.class),mock(WhatsAppService.class));
        row=new AtomicReference<>();deliveredCode=new AtomicReference<>();
        when(repo.findTopByTargetAndPurposeOrderByCreatedAtDesc(anyString(),anyString())).thenAnswer(i -> Optional.ofNullable(row.get()).filter(r -> r.getTarget().equals(i.getArgument(0)) && r.getPurpose().equals(i.getArgument(1))));
        when(repo.findByTargetAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(anyString(),anyString())).thenReturn(List.of());
        when(repo.save(any())).thenAnswer(i -> { row.set(i.getArgument(0));return row.get(); });
        doAnswer(i -> { deliveredCode.set(i.getArgument(1));return null; }).when(email).sendOtpEmail(anyString(),anyString(),anyString());
    }
    @Test void deliveryStoresAHashAndReturnsNoCode() {
        var result=service.sendOtp(" Owner@Example.test ","REGISTRATION");
        assertNull(result.otpCode());assertEquals("owner@example.test",row.get().getTarget());
        assertTrue(deliveredCode.get().matches("[0-9]{6}"));assertNotEquals(deliveredCode.get(),row.get().getOtpCode());
        assertTrue(row.get().getOtpCode().startsWith("$2"));
    }
    @Test void verifiedReceiptIsBoundToEmailAndCanOnlyBeUsedOnce() {
        service.sendOtp("owner@example.test","REGISTRATION");
        String proof=service.verifyEmailRegistration("OWNER@EXAMPLE.TEST",deliveredCode.get());
        assertNotNull(proof);assertNotEquals(proof,row.get().getVerificationTokenHash());
        assertFalse(service.consumeEmailVerification("other@example.test",proof));
        assertFalse(service.consumeEmailVerification("owner@example.test","x".repeat(43)));
        assertNull(service.verifyEmailRegistration("owner@example.test",deliveredCode.get()));
        assertTrue(service.consumeEmailVerification("owner@example.test",proof));
        assertFalse(service.consumeEmailVerification("owner@example.test",proof));
    }
    @Test void fiveWrongAttemptsLockTheCode() {
        service.sendOtp("owner@example.test","REGISTRATION");
        for(int i=0;i<5;i++) assertNull(service.verifyEmailRegistration("owner@example.test","000000"));
        assertNull(service.verifyEmailRegistration("owner@example.test",deliveredCode.get()));
    }
    @Test void expiredCodesAndReceiptsAreRejected() {
        service.sendOtp("owner@example.test","REGISTRATION");row.get().setExpiresAt(Instant.now().minusSeconds(1));
        assertNull(service.verifyEmailRegistration("owner@example.test",deliveredCode.get()));
        row.get().setExpiresAt(Instant.now().plusSeconds(60));
        String proof=service.verifyEmailRegistration("owner@example.test",deliveredCode.get());
        row.get().setExpiresAt(Instant.now().minusSeconds(1));assertFalse(service.consumeEmailVerification("owner@example.test",proof));
    }
    @Test void resendCooldownAndDeliveryFailuresAreVisible() {
        service.sendOtp("owner@example.test","REGISTRATION");
        assertThrows(IllegalArgumentException.class,()->service.sendOtp("owner@example.test","REGISTRATION"));
        row.set(null);doThrow(new IllegalStateException("provider unavailable")).when(email).sendOtpEmail(anyString(),anyString(),anyString());
        assertThrows(IllegalStateException.class,()->service.sendOtp("owner@example.test","REGISTRATION"));
    }
}
