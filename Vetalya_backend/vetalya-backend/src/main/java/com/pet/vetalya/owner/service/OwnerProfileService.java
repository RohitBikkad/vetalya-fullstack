package com.pet.vetalya.owner.service;

import com.pet.vetalya.common.GlobalExceptionHandler.ResourceNotFoundException;
import com.pet.vetalya.entity.Appointment;
import com.pet.vetalya.entity.PetOwner;
import com.pet.vetalya.entity.UserAuth;
import com.pet.vetalya.owner.dto.OwnerProfileRequest;
import com.pet.vetalya.owner.dto.OwnerProfileResponse;
import com.pet.vetalya.repository.AppointmentRepository;
import com.pet.vetalya.repository.PetOwnerRepository;
import com.pet.vetalya.repository.RefreshTokenRepository;
import com.pet.vetalya.repository.ReminderRepository;
import com.pet.vetalya.repository.UserAuthRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class OwnerProfileService {

    private static final Logger log = LoggerFactory.getLogger(OwnerProfileService.class);

    private final PetOwnerRepository petOwnerRepo;
    private final UserAuthRepository userAuthRepo;
    private final RefreshTokenRepository refreshTokenRepo;
    private final AppointmentRepository appointmentRepo;
    private final ReminderRepository reminderRepo;

    public OwnerProfileService(PetOwnerRepository petOwnerRepo,
                               UserAuthRepository userAuthRepo,
                               RefreshTokenRepository refreshTokenRepo,
                               AppointmentRepository appointmentRepo,
                               ReminderRepository reminderRepo) {
        this.petOwnerRepo = petOwnerRepo;
        this.userAuthRepo = userAuthRepo;
        this.refreshTokenRepo = refreshTokenRepo;
        this.appointmentRepo = appointmentRepo;
        this.reminderRepo = reminderRepo;
    }

    /**
     * POST /api/owner/profile — Create or fill my profile.
     * My pet_owner row may already exist from signup, so handle both cases.
     */
    @Transactional
    public OwnerProfileResponse createOrFillProfile(String userId, OwnerProfileRequest request) {
        UserAuth user = getUserAuth(userId);
        PetOwner owner = petOwnerRepo.findById(userId).orElseGet(() -> {
            PetOwner newOwner = new PetOwner();
            newOwner.setUserId(userId);
            return newOwner;
        });

        applyFields(owner, request);
        owner = petOwnerRepo.save(owner);
        log.info("Pet owner profile created/filled: userId={}", userId);

        return toResponse(owner, user);
    }

    /**
     * GET /api/owner/profile — Get my profile with completion flag and user_auth details.
     */
    @Transactional(readOnly = true)
    public OwnerProfileResponse getProfile(String userId) {
        UserAuth user = getUserAuth(userId);
        PetOwner owner = petOwnerRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet owner profile not found"));
        return toResponse(owner, user);
    }

    /**
     * PUT /api/owner/profile — Update my profile.
     */
    @Transactional
    public OwnerProfileResponse updateProfile(String userId, OwnerProfileRequest request) {
        UserAuth user = getUserAuth(userId);
        PetOwner owner = petOwnerRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet owner profile not found"));

        applyFields(owner, request);
        owner = petOwnerRepo.save(owner);
        log.info("Pet owner profile updated: userId={}", userId);

        return toResponse(owner, user);
    }

    /**
     * DELETE /api/owner/account — Soft delete account in one transaction:
     * 1. Set user_auth.status = DELETED
     * 2. Bump token_version
     * 3. Revoke all refresh_token rows
     * 4. Cancel future BOOKED/CONFIRMED appointments
     * 5. Delete my unsent reminders
     */
    @Transactional
    public void deleteAccount(String userId) {
        UserAuth user = getUserAuth(userId);

        user.setStatus("DELETED");
        user.setTokenVersion(user.getTokenVersion() + 1);
        userAuthRepo.save(user);

        refreshTokenRepo.revokeAllByUserId(userId);

        // Cancel all future booked/confirmed appointments
        List<Appointment> futureAppointments = appointmentRepo.findFutureBookedOrConfirmedForOwner(userId, Instant.now());
        for (Appointment a : futureAppointments) {
            a.setStatus("CANCELLED");
            appointmentRepo.save(a);
            appointmentRepo.deletePendingReminders(a.getAppointmentId());
        }

        // Delete all unsent reminders for this owner
        reminderRepo.deleteAllUnsentByUserId(userId);

        log.info("Owner account soft-deleted: userId={}, cancelled {} future appointments",
                userId, futureAppointments.size());
    }

    private UserAuth getUserAuth(String userId) {
        return userAuthRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void applyFields(PetOwner owner, OwnerProfileRequest r) {
        owner.setFirstName(r.firstName());
        owner.setLastName(r.lastName());
        owner.setAddress1(r.address1());
        owner.setAddress2(r.address2());
        owner.setPincode(r.pincode());
        owner.setCity(r.city());
        owner.setState(r.state());
        owner.setCountry(r.country() != null ? r.country() : "INDIA");
    }

    private OwnerProfileResponse toResponse(PetOwner owner, UserAuth user) {
        boolean complete = owner.getFirstName() != null && !owner.getFirstName().isBlank()
                && owner.getLastName() != null && !owner.getLastName().isBlank()
                && owner.getPincode() != null && !owner.getPincode().isBlank()
                && owner.getCity() != null && !owner.getCity().isBlank()
                && owner.getState() != null && !owner.getState().isBlank();

        return new OwnerProfileResponse(
                owner.getUserId(),
                owner.getFirstName(),
                owner.getLastName(),
                owner.getAddress1(),
                owner.getAddress2(),
                owner.getPincode(),
                owner.getCity(),
                owner.getState(),
                owner.getCountry(),
                complete,
                user.getEmail(),
                user.isEmailVerified(),
                user.getPhone(),
                user.isPhoneVerified(),
                user.getStatus(),
                owner.getCreatedAt(),
                owner.getUpdatedAt()
        );
    }
}
