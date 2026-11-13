package com.pet.vetalya.repository;

import com.pet.vetalya.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<OtpVerification> findTopByTargetAndPurposeOrderByCreatedAtDesc(String target, String purpose);
    List<OtpVerification> findByTargetAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(String target, String purpose);
    Optional<OtpVerification> findTopByTargetAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(String target, String purpose);
    Optional<OtpVerification> findTopByTargetAndPurposeAndVerifiedTrueOrderByCreatedAtDesc(String target, String purpose);
}
