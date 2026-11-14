package com.pet.vetalya.vet;

import com.pet.vetalya.common.GlobalExceptionHandler.BadRequestException;
import com.pet.vetalya.common.GlobalExceptionHandler.ConflictException;
import com.pet.vetalya.common.GlobalExceptionHandler.ForbiddenException;
import com.pet.vetalya.common.GlobalExceptionHandler.ResourceNotFoundException;
import com.pet.vetalya.common.IdGenerator;
import com.pet.vetalya.entity.Appointment;
import com.pet.vetalya.entity.Hospital;
import com.pet.vetalya.entity.UserAuth;
import com.pet.vetalya.entity.Vet;
import com.pet.vetalya.repository.*;
import com.pet.vetalya.vet.dto.HospitalRequest;
import com.pet.vetalya.vet.dto.VaccinationRequest;
import com.pet.vetalya.vet.dto.VetSearchResult;
import com.pet.vetalya.vet.service.AppointmentService;
import com.pet.vetalya.vet.service.HospitalService;
import com.pet.vetalya.vet.service.VaccinationService;
import com.pet.vetalya.vet.service.VetProfileService;
import com.pet.vetalya.vet.service.VetSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VetServiceBusinessLogicTest {

    @Mock private com.pet.vetalya.clinic.ClinicalAccess clinicalAccess;
    @Mock private HospitalRepository hospitalRepo;
    @Mock private AppointmentRepository appointmentRepo;
    @Mock private UserAuthRepository userAuthRepo;
    @Mock private RefreshTokenRepository refreshTokenRepo;
    @Mock private VetRepository vetRepo;
    @Mock private PetRepository petRepo;
    @Mock private VaccinationRepository vaccinationRepo;
    @Mock private ReviewRepository reviewRepo;
    @Mock private IdGenerator idGenerator;

    private HospitalService hospitalService;
    private AppointmentService appointmentService;
    private VaccinationService vaccinationService;
    private VetProfileService vetProfileService;
    private VetSearchService vetSearchService;

    @BeforeEach
    void setUp() {
        hospitalService = new HospitalService(hospitalRepo, appointmentRepo, idGenerator);
        appointmentService = new AppointmentService(appointmentRepo, reviewRepo, null, null, clinicalAccess);
        vaccinationService = new VaccinationService(vaccinationRepo, appointmentRepo, petRepo, idGenerator);
        vetProfileService = new VetProfileService(vetRepo, userAuthRepo, appointmentRepo, refreshTokenRepo);
        vetSearchService = new VetSearchService(vetRepo, hospitalRepo, reviewRepo, userAuthRepo);
    }

    @Test
    @DisplayName("Scenario 1: A vet cannot read or modify another vet's hospital (returns 404)")
    void vetCannotReadOrModifyAnotherVetsHospital() {
        when(hospitalRepo.findByHospitalIdAndVetUserIdAndActiveTrue("hosp_other", "vet_1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> hospitalService.getOwn("vet_1", "hosp_other"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Hospital not found");

        HospitalRequest updateReq = new HospitalRequest(
                "Updated Clinic", "Street 1", null, "Mumbai", "MH", "400001",
                "INDIA", "+919876543210", "vet@test.com", null, true, null, null);

        assertThatThrownBy(() -> hospitalService.update("vet_1", "hosp_other", updateReq))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Hospital not found");
    }

    @Test
    @DisplayName("Scenario 3: Hospital 24x7 timing validation")
    void hospitalTimingValidation() {
        when(hospitalRepo.countByVetUserIdAndActiveTrue("vet_1")).thenReturn(0L);

        // When not 24x7, openTime and closeTime must be provided
        HospitalRequest missingTimes = new HospitalRequest(
                "Clinic A", "Street 1", null, "Mumbai", "MH", "400001",
                "INDIA", "+919876543210", "vet@test.com", null, false, null, null);

        assertThatThrownBy(() -> hospitalService.create("vet_1", missingTimes))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Open time and close time are required");

        // When not 24x7, openTime must be before closeTime
        HospitalRequest invalidOrder = new HospitalRequest(
                "Clinic A", "Street 1", null, "Mumbai", "MH", "400001",
                "INDIA", "+919876543210", "vet@test.com", null, false,
                LocalTime.of(18, 0), LocalTime.of(9, 0));

        assertThatThrownBy(() -> hospitalService.create("vet_1", invalidOrder))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Open time must be before close time");

        // Valid 24x7
        when(idGenerator.newId()).thenReturn("hosp_123");
        Hospital valid24x7 = new Hospital();
        valid24x7.setHospitalId("hosp_123");
        valid24x7.setVetUserId("vet_1");
        valid24x7.setName("Clinic 24x7");
        valid24x7.setCity("Mumbai");
        valid24x7.setIs24x7(true);
        when(hospitalRepo.save(any())).thenReturn(valid24x7);

        HospitalRequest req24x7 = new HospitalRequest(
                "Clinic 24x7", "Street 1", null, "Mumbai", "MH", "400001",
                "INDIA", "+919876543210", "vet@test.com", null, true, null, null);

        var res = hospitalService.create("vet_1", req24x7);
        assertThat(res.is24x7()).isTrue();
    }

    @Test
    @DisplayName("Scenario 4: Invalid appointment status transition returns 409")
    void invalidAppointmentStatusTransition() {
        Appointment apt = new Appointment();
        apt.setAppointmentId("apt_1");
        apt.setVetUserId("vet_1");
        apt.setStatus("BOOKED");
        apt.setAppointmentAt(Instant.now().plusSeconds(3600));

        when(appointmentRepo.findByAppointmentIdAndVetUserId("apt_1", "vet_1"))
                .thenReturn(Optional.of(apt));

        // BOOKED -> COMPLETED is invalid (must go BOOKED -> CONFIRMED -> COMPLETED)
        assertThatThrownBy(() -> appointmentService.updateStatus("vet_1", "apt_1", "COMPLETED", null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot transition from BOOKED to COMPLETED");

        // COMPLETED before appointment_at has passed is invalid
        apt.setStatus("CONFIRMED");
        apt.setAppointmentAt(Instant.now().plusSeconds(3600)); // future
        assertThatThrownBy(() -> appointmentService.updateStatus("vet_1", "apt_1", "COMPLETED", null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("before the appointment time has passed");
    }

    @Test
    @DisplayName("Scenario 6: Vaccination blocked for a pet with no non-cancelled appointment with this vet")
    void vaccinationBlockedWithoutAppointment() {
        when(petRepo.existsById("pet_99")).thenReturn(true);
        when(appointmentRepo.existsNonCancelledForVetAndPet("vet_1", "pet_99")).thenReturn(false);

        VaccinationRequest req = new VaccinationRequest(
                "Rabies", "BATCH1", LocalDate.now(), LocalDate.now().plusYears(1), null);

        assertThatThrownBy(() -> vaccinationService.create("vet_1", "pet_99", req))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Vet must have at least one non-cancelled appointment");
    }

    @Test
    @DisplayName("Scenario 7: Soft delete cancels future appointments and revokes refresh tokens")
    void softDeleteCancelsFutureAppointmentsAndRevokesTokens() {
        UserAuth user = new UserAuth();
        user.setUserId("vet_1");
        user.setStatus("ACTIVE");
        user.setTokenVersion(1);
        when(userAuthRepo.findById("vet_1")).thenReturn(Optional.of(user));

        Appointment futureApt = new Appointment();
        futureApt.setAppointmentId("apt_future");
        futureApt.setVetUserId("vet_1");
        futureApt.setStatus("CONFIRMED");
        when(appointmentRepo.findFutureBookedOrConfirmed(eq("vet_1"), any()))
                .thenReturn(List.of(futureApt));

        vetProfileService.deleteAccount("vet_1");

        // Status updated to DELETED and tokenVersion incremented
        assertThat(user.getStatus()).isEqualTo("DELETED");
        assertThat(user.getTokenVersion()).isEqualTo(2);
        verify(refreshTokenRepo).revokeAllByUserId("vet_1");

        // Future appointment cancelled and reminders deleted
        assertThat(futureApt.getStatus()).isEqualTo("CANCELLED");
        verify(appointmentRepo).save(futureApt);
        verify(appointmentRepo).deletePendingReminders("apt_future");
    }

    @Test
    @DisplayName("Scenario 8: Search returns only verified, active vets and no private fields")
    void searchReturnsOnlyVerifiedActiveVets() {
        Vet verifiedVet = new Vet();
        verifiedVet.setUserId("vet_verified");
        verifiedVet.setFirstName("Dr. John");
        verifiedVet.setLastName("Doe");
        verifiedVet.setVerified(true);

        Vet unverifiedVet = new Vet();
        unverifiedVet.setUserId("vet_unverified");
        unverifiedVet.setVerified(false);

        when(vetRepo.findAll()).thenReturn(List.of(verifiedVet, unverifiedVet));

        UserAuth user = new UserAuth();
        user.setUserId("vet_verified");
        user.setStatus("ACTIVE");
        user.setEmail("private@vet.com");
        when(userAuthRepo.findById("vet_verified")).thenReturn(Optional.of(user));

        List<VetSearchResult> results = vetSearchService.search(null);

        assertThat(results).hasSize(1);
        VetSearchResult result = results.get(0);
        assertThat(result.vetUserId()).isEqualTo("vet_verified");
        assertThat(result.firstName()).isEqualTo("Dr. John");

        // Verify public DTO does not leak private fields
        // VetSearchResult has no email, phone, registrationNo, dob fields
        Class<?> clazz = VetSearchResult.class;
        assertThat(clazz.getDeclaredFields()).noneMatch(f ->
                f.getName().equalsIgnoreCase("email") ||
                f.getName().equalsIgnoreCase("phone") ||
                f.getName().equalsIgnoreCase("registrationNo") ||
                f.getName().equalsIgnoreCase("dob"));
    }
}
