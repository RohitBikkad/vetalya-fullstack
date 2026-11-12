package com.pet.vetalya.vet.service;

import com.pet.vetalya.common.GlobalExceptionHandler.ForbiddenException;
import com.pet.vetalya.common.GlobalExceptionHandler.ResourceNotFoundException;
import com.pet.vetalya.entity.Appointment;
import com.pet.vetalya.entity.UserAuth;
import com.pet.vetalya.entity.Vet;
import com.pet.vetalya.repository.AppointmentRepository;
import com.pet.vetalya.repository.RefreshTokenRepository;
import com.pet.vetalya.repository.UserAuthRepository;
import com.pet.vetalya.repository.VetRepository;
import com.pet.vetalya.vet.dto.VetProfileRequest;
import com.pet.vetalya.vet.dto.VetProfileResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class VetProfileService {

    private static final Logger log = LoggerFactory.getLogger(VetProfileService.class);

    private final VetRepository vetRepo;
    private final UserAuthRepository userAuthRepo;
    private final AppointmentRepository appointmentRepo;
    private final RefreshTokenRepository refreshTokenRepo;

    public VetProfileService(VetRepository vetRepo,
                             UserAuthRepository userAuthRepo,
                             AppointmentRepository appointmentRepo,
                             RefreshTokenRepository refreshTokenRepo) {
        this.vetRepo = vetRepo;
        this.userAuthRepo = userAuthRepo;
        this.appointmentRepo = appointmentRepo;
        this.refreshTokenRepo = refreshTokenRepo;
    }

    /**
     * Create / fill vet profile (first time after OAuth login).
     */
    @Transactional
    public VetProfileResponse createProfile(String userId, VetProfileRequest request) {
        Vet vet = vetRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vet record not found for user " + userId));

        vet.setFirstName(request.firstName());
        vet.setLastName(request.lastName());
        vet.setRegistrationNo(request.registrationNo());
        vet.setDob(request.dob());
        vet.setPhotoUrl(request.photoUrl());

        vet = vetRepo.save(vet);
        log.info("Vet profile created/filled for userId={}", userId);

        return toResponse(vet, getUserAuth(userId));
    }

    /**
     * Get vet profile with auth details.
     */
    @Transactional(readOnly = true)
    public VetProfileResponse getProfile(String userId) {
        Vet vet = vetRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vet profile not found"));
        return toResponse(vet, getUserAuth(userId));
    }

    /**
     * Update vet profile. registration_no is only changeable if is_verified = false.
     */
    @Transactional
    public VetProfileResponse updateProfile(String userId, VetProfileRequest request) {
        Vet vet = vetRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vet profile not found"));

        // If vet is verified, registration_no cannot be changed
        if (vet.isVerified() && request.registrationNo() != null
                && !request.registrationNo().equals(vet.getRegistrationNo())) {
            throw new ForbiddenException("Registration number cannot be changed after verification");
        }

        vet.setFirstName(request.firstName());
        vet.setLastName(request.lastName());
        if (!vet.isVerified()) {
            vet.setRegistrationNo(request.registrationNo());
        }
        vet.setDob(request.dob());
        vet.setPhotoUrl(request.photoUrl());

        vet = vetRepo.save(vet);
        log.info("Vet profile updated for userId={}", userId);

        return toResponse(vet, getUserAuth(userId));
    }

    /**
     * Soft delete vet account:
     * 1. Mark status = DELETED
     * 2. Bump token_version
     * 3. Revoke all refresh tokens
     * 4. Cancel future BOOKED/CONFIRMED appointments
     */
    @Transactional
    public void deleteAccount(String userId) {
        UserAuth user = getUserAuth(userId);

        user.setStatus("DELETED");
        user.setTokenVersion(user.getTokenVersion() + 1);
        userAuthRepo.save(user);

        refreshTokenRepo.revokeAllByUserId(userId);

        // Cancel all future booked/confirmed appointments
        List<Appointment> futureAppointments = appointmentRepo.findFutureBookedOrConfirmed(userId, Instant.now());
        for (Appointment a : futureAppointments) {
            a.setStatus("CANCELLED");
            appointmentRepo.save(a);
            // Delete pending unsent reminders
            appointmentRepo.deletePendingReminders(a.getAppointmentId());
        }

        log.info("Vet account soft-deleted: userId={}, cancelled {} future appointments",
                userId, futureAppointments.size());
    }

    private UserAuth getUserAuth(String userId) {
        return userAuthRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private VetProfileResponse toResponse(Vet vet, UserAuth user) {
        return new VetProfileResponse(
                vet.getUserId(),
                vet.getFirstName(),
                vet.getLastName(),
                vet.getRegistrationNo(),
                vet.getDob(),
                vet.getPhotoUrl(),
                vet.isVerified(),
                vet.isProfileComplete(),
                user.getEmail(),
                user.isEmailVerified(),
                user.getPhone(),
                user.isPhoneVerified(),
                user.getStatus(),
                vet.getCreatedAt(),
                vet.getUpdatedAt()
        );
    }
}
