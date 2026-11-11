package com.pet.vetalya.owner;

import com.pet.vetalya.common.GlobalExceptionHandler.BadRequestException;
import com.pet.vetalya.common.GlobalExceptionHandler.ConflictException;
import com.pet.vetalya.common.GlobalExceptionHandler.ResourceNotFoundException;
import com.pet.vetalya.common.IdGenerator;
import com.pet.vetalya.config.AppProperties;
import com.pet.vetalya.entity.*;
import com.pet.vetalya.owner.dto.AppointmentBookingRequest;
import com.pet.vetalya.owner.dto.AppointmentRescheduleRequest;
import com.pet.vetalya.owner.dto.PetRequest;
import com.pet.vetalya.owner.dto.ReviewRequest;
import com.pet.vetalya.owner.service.*;
import com.pet.vetalya.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OwnerServiceBusinessLogicTest {

    @Mock private PetRepository petRepo;
    @Mock private AppointmentRepository appointmentRepo;
    @Mock private VaccinationRepository vaccinationRepo;
    @Mock private VetRepository vetRepo;
    @Mock private UserAuthRepository userAuthRepo;
    @Mock private HospitalRepository hospitalRepo;
    @Mock private ReminderRepository reminderRepo;
    @Mock private ReviewRepository reviewRepo;
    @Mock private PetOwnerRepository petOwnerRepo;
    @Mock private RefreshTokenRepository refreshTokenRepo;
    @Mock private IdGenerator idGenerator;

    private AppProperties appProperties;
    private PetService petService;
    private OwnerAppointmentService appointmentService;
    private OwnerReviewService reviewService;
    private AvailabilityService availabilityService;
    private OwnerProfileService profileService;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties(
                "http://localhost:3000",
                "myvetapp",
                new AppProperties.Jwt("k1", "k2", 15, 14),
                new AppProperties.Google(List.of()),
                new AppProperties.Apple("t", "k", "s", "b", "p"),
                new AppProperties.Cors("http://localhost:3000"),
                new AppProperties.Appointment(30, 60, 2, List.of(24, 1)),
                new AppProperties.Pet(20),
                new AppProperties.Review(30),
                "Asia/Kolkata"
        );

        petService = new PetService(petRepo, appointmentRepo, vaccinationRepo, idGenerator, appProperties);
        appointmentService = new OwnerAppointmentService(
                appointmentRepo, petRepo, vetRepo, userAuthRepo, hospitalRepo, reminderRepo, idGenerator, appProperties);
        reviewService = new OwnerReviewService(
                reviewRepo, appointmentRepo, vetRepo, hospitalRepo, idGenerator, appProperties);
        availabilityService = new AvailabilityService(
                vetRepo, userAuthRepo, hospitalRepo, appointmentRepo, appProperties);
        profileService = new OwnerProfileService(
                petOwnerRepo, userAuthRepo, refreshTokenRepo, appointmentRepo, reminderRepo);
    }

    @Test
    @DisplayName("Scenario 1: An owner cannot read, edit or delete another owner's pet (404)")
    void ownerCannotAccessAnotherOwnersPet() {
        when(petRepo.findByPetIdAndOwnerUserIdAndDeletedAtIsNull("pet_other", "owner_1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> petService.getPet("owner_1", "pet_other"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Pet not found");

        PetRequest updateReq = new PetRequest(
                "Tommy", "DOG", "MALE", LocalDate.of(2023, 1, 1), "Labrador", null, null);

        assertThatThrownBy(() -> petService.updatePet("owner_1", "pet_other", updateReq))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Pet not found");

        assertThatThrownBy(() -> petService.deletePet("owner_1", "pet_other"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Pet not found");
    }

    @Test
    @DisplayName("Scenario 2: Booking with another owner's pet is rejected (404)")
    void bookingWithAnotherOwnersPetRejected() {
        when(petRepo.findByPetIdAndOwnerUserIdAndDeletedAtIsNull("pet_other", "owner_1"))
                .thenReturn(Optional.empty());

        AppointmentBookingRequest req = new AppointmentBookingRequest(
                "vet_1", "hosp_1", "pet_other", Instant.now().plus(Duration.ofHours(5)), null);

        assertThatThrownBy(() -> appointmentService.bookAppointment("owner_1", req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Pet not found");
    }

    @Test
    @DisplayName("Scenario 3: Booking outside opening hours is rejected (400)")
    void bookingOutsideOpeningHoursRejected() {
        Pet pet = new Pet();
        pet.setPetId("pet_1");
        pet.setName("Rex");
        when(petRepo.findByPetIdAndOwnerUserIdAndDeletedAtIsNull("pet_1", "owner_1"))
                .thenReturn(Optional.of(pet));

        Vet vet = new Vet();
        vet.setUserId("vet_1");
        vet.setVerified(true);
        when(vetRepo.findById("vet_1")).thenReturn(Optional.of(vet));

        UserAuth vetAuth = new UserAuth();
        vetAuth.setStatus("ACTIVE");
        when(userAuthRepo.findById("vet_1")).thenReturn(Optional.of(vetAuth));

        Hospital hospital = new Hospital();
        hospital.setHospitalId("hosp_1");
        hospital.setVetUserId("vet_1");
        hospital.setIs24x7(false);
        hospital.setOpenTime(LocalTime.of(9, 0));
        hospital.setCloseTime(LocalTime.of(17, 0)); // Closes at 17:00
        when(hospitalRepo.findByHospitalIdAndVetUserIdAndActiveTrue("hosp_1", "vet_1"))
                .thenReturn(Optional.of(hospital));

        // Attempt to book at 22:00 IST tomorrow (well outside 09:00 - 17:00)
        LocalDate tomorrow = LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(1);
        Instant outsideHours = tomorrow.atTime(LocalTime.of(22, 0)).atZone(ZoneId.of("Asia/Kolkata")).toInstant();

        AppointmentBookingRequest req = new AppointmentBookingRequest(
                "vet_1", "hosp_1", "pet_1", outsideHours, "Checkup");

        assertThatThrownBy(() -> appointmentService.bookAppointment("owner_1", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("outside the hospital's opening hours");
    }

    @Test
    @DisplayName("Scenario 4: Double-booking slot conflict results in 409")
    void doubleBookingConflictResultsIn409() {
        Pet pet = new Pet();
        pet.setPetId("pet_1");
        when(petRepo.findByPetIdAndOwnerUserIdAndDeletedAtIsNull("pet_1", "owner_1"))
                .thenReturn(Optional.of(pet));

        Vet vet = new Vet();
        vet.setUserId("vet_1");
        vet.setVerified(true);
        when(vetRepo.findById("vet_1")).thenReturn(Optional.of(vet));

        UserAuth vetAuth = new UserAuth();
        vetAuth.setStatus("ACTIVE");
        when(userAuthRepo.findById("vet_1")).thenReturn(Optional.of(vetAuth));

        Hospital hospital = new Hospital();
        hospital.setHospitalId("hosp_1");
        hospital.setVetUserId("vet_1");
        hospital.setIs24x7(true);
        when(hospitalRepo.findByHospitalIdAndVetUserIdAndActiveTrue("hosp_1", "vet_1"))
                .thenReturn(Optional.of(hospital));

        LocalDate tomorrow = LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(1);
        Instant slot = tomorrow.atTime(LocalTime.of(10, 0)).atZone(ZoneId.of("Asia/Kolkata")).toInstant();

        // 1. Slot already booked check in database
        when(appointmentRepo.existsSlotForVet("vet_1", slot)).thenReturn(true);

        AppointmentBookingRequest req = new AppointmentBookingRequest(
                "vet_1", "hosp_1", "pet_1", slot, null);

        assertThatThrownBy(() -> appointmentService.bookAppointment("owner_1", req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Slot no longer available");

        // 2. Or concurrent DB unique index collision
        when(appointmentRepo.existsSlotForVet("vet_1", slot)).thenReturn(false);
        when(appointmentRepo.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uq_appointment_vet_slot"));

        assertThatThrownBy(() -> appointmentService.bookAppointment("owner_1", req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Slot no longer available");
    }

    @Test
    @DisplayName("Scenario 5: Cancel inside cutoff window returns 409")
    void cancelInsideCutoffWindowReturns409() {
        Appointment apt = new Appointment();
        apt.setAppointmentId("apt_1");
        apt.setOwnerUserId("owner_1");
        apt.setStatus("CONFIRMED");
        // Appointment is 30 minutes from now (cutoff is 2 hours)
        apt.setAppointmentAt(Instant.now().plus(Duration.ofMinutes(30)));

        when(appointmentRepo.findByAppointmentIdAndOwnerUserId("apt_1", "owner_1"))
                .thenReturn(Optional.of(apt));

        assertThatThrownBy(() -> appointmentService.cancel("owner_1", "apt_1", null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("within 2 hours of scheduled time");
    }

    @Test
    @DisplayName("Scenario 6: Reschedule resets status to BOOKED and replaces reminders")
    void rescheduleResetsStatusAndReplacesReminders() {
        Appointment apt = new Appointment();
        apt.setAppointmentId("apt_1");
        apt.setOwnerUserId("owner_1");
        apt.setPetId("pet_1");
        apt.setVetUserId("vet_1");
        apt.setStatus("CONFIRMED");
        apt.setAppointmentAt(Instant.now().plus(Duration.ofDays(2)));

        when(appointmentRepo.findByAppointmentIdAndOwnerUserId("apt_1", "owner_1"))
                .thenReturn(Optional.of(apt));

        LocalDate newDate = LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(3);
        Instant newSlot = newDate.atTime(LocalTime.of(11, 0)).atZone(ZoneId.of("Asia/Kolkata")).toInstant();

        when(appointmentRepo.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        AppointmentRescheduleRequest req = new AppointmentRescheduleRequest(newSlot);
        appointmentService.reschedule("owner_1", "apt_1", req);

        // Status reset to BOOKED
        assertThat(apt.getStatus()).isEqualTo("BOOKED");
        assertThat(apt.getAppointmentAt()).isEqualTo(newSlot);

        // Old reminders deleted and new ones created
        verify(reminderRepo).deleteUnsentByAppointmentId("apt_1");
        verify(reminderRepo, atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("Scenario 7: Review rejected for non-COMPLETED appointment and for duplicate")
    void reviewRejectedForNonCompletedOrDuplicate() {
        Appointment apt = new Appointment();
        apt.setAppointmentId("apt_1");
        apt.setOwnerUserId("owner_1");
        apt.setStatus("BOOKED"); // Not completed
        when(appointmentRepo.findByAppointmentIdAndOwnerUserId("apt_1", "owner_1"))
                .thenReturn(Optional.of(apt));

        ReviewRequest req = new ReviewRequest("Great", "Awesome vet", 5);

        // Non-COMPLETED rejected
        assertThatThrownBy(() -> reviewService.createReview("owner_1", "apt_1", req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Reviews can only be submitted for COMPLETED appointments");

        // Duplicate review rejected
        apt.setStatus("COMPLETED");
        when(reviewRepo.existsByAppointmentIdAndOwnerUserId("apt_1", "owner_1")).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview("owner_1", "apt_1", req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already reviewed this appointment");
    }

    @Test
    @DisplayName("Scenario 8: Pet soft delete blocked by future appointments")
    void petSoftDeleteBlockedByFutureAppointments() {
        Pet pet = new Pet();
        pet.setPetId("pet_1");
        pet.setOwnerUserId("owner_1");
        when(petRepo.findByPetIdAndOwnerUserIdAndDeletedAtIsNull("pet_1", "owner_1"))
                .thenReturn(Optional.of(pet));

        when(appointmentRepo.existsFutureBookedOrConfirmedForPet(eq("pet_1"), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> petService.deletePet("owner_1", "pet_1"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot delete pet with future appointments");
    }

    @Test
    @DisplayName("Scenario 9: Account deletion cancels future appointments and revokes refresh tokens")
    void accountDeletionCancelsAppointmentsAndRevokesTokens() {
        UserAuth user = new UserAuth();
        user.setUserId("owner_1");
        user.setStatus("ACTIVE");
        user.setTokenVersion(1);
        when(userAuthRepo.findById("owner_1")).thenReturn(Optional.of(user));

        Appointment apt = new Appointment();
        apt.setAppointmentId("apt_1");
        apt.setStatus("CONFIRMED");
        when(appointmentRepo.findFutureBookedOrConfirmedForOwner(eq("owner_1"), any()))
                .thenReturn(List.of(apt));

        profileService.deleteAccount("owner_1");

        assertThat(user.getStatus()).isEqualTo("DELETED");
        assertThat(user.getTokenVersion()).isEqualTo(2);
        verify(refreshTokenRepo).revokeAllByUserId("owner_1");
        assertThat(apt.getStatus()).isEqualTo("CANCELLED");
        verify(reminderRepo).deleteAllUnsentByUserId("owner_1");
    }

    @Test
    @DisplayName("Scenario 10: Availability excludes taken and past slots")
    void availabilityExcludesTakenAndPastSlots() {
        Vet vet = new Vet();
        vet.setUserId("vet_1");
        vet.setVerified(true);
        when(vetRepo.findById("vet_1")).thenReturn(Optional.of(vet));

        UserAuth vetAuth = new UserAuth();
        vetAuth.setStatus("ACTIVE");
        when(userAuthRepo.findById("vet_1")).thenReturn(Optional.of(vetAuth));

        Hospital hospital = new Hospital();
        hospital.setHospitalId("hosp_1");
        hospital.setVetUserId("vet_1");
        hospital.setIs24x7(false);
        hospital.setOpenTime(LocalTime.of(10, 0));
        hospital.setCloseTime(LocalTime.of(11, 30)); // 3 slots of 30 mins: 10:00, 10:30, 11:00
        when(hospitalRepo.findByHospitalIdAndVetUserIdAndActiveTrue("hosp_1", "vet_1"))
                .thenReturn(Optional.of(hospital));

        LocalDate tomorrow = LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(1);
        Instant bookedSlot = tomorrow.atTime(LocalTime.of(10, 30)).atZone(ZoneId.of("Asia/Kolkata")).toInstant();
        when(appointmentRepo.findBookedSlotsForVet(eq("vet_1"), any(), any()))
                .thenReturn(List.of(bookedSlot));

        var freeSlots = availabilityService.getFreeSlots("vet_1", tomorrow, "hosp_1");

        // Total 3 slots, 10:30 is booked, so 2 free slots: 10:00 and 11:00
        assertThat(freeSlots).hasSize(2);
        assertThat(freeSlots.stream().map(s -> s.slotStart())).doesNotContain(bookedSlot);
    }
}
