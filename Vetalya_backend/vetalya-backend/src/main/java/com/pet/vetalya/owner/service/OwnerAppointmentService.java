package com.pet.vetalya.owner.service;

import com.pet.vetalya.common.GlobalExceptionHandler.BadRequestException;
import com.pet.vetalya.common.GlobalExceptionHandler.ConflictException;
import com.pet.vetalya.common.GlobalExceptionHandler.ResourceNotFoundException;
import com.pet.vetalya.common.IdGenerator;
import com.pet.vetalya.common.PageResponse;
import com.pet.vetalya.config.AppProperties;
import com.pet.vetalya.entity.*;
import com.pet.vetalya.owner.dto.*;
import com.pet.vetalya.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.Set;

@Service
public class OwnerAppointmentService {

    private static final Logger log = LoggerFactory.getLogger(OwnerAppointmentService.class);
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("appointmentAt", "createdAt", "status");

    private final AppointmentRepository appointmentRepo;
    private final PetRepository petRepo;
    private final VetRepository vetRepo;
    private final UserAuthRepository userAuthRepo;
    private final HospitalRepository hospitalRepo;
    private final ReminderRepository reminderRepo;
    private final IdGenerator idGenerator;
    private final AppProperties appProperties;

    public OwnerAppointmentService(AppointmentRepository appointmentRepo,
                                   PetRepository petRepo,
                                   VetRepository vetRepo,
                                   UserAuthRepository userAuthRepo,
                                   HospitalRepository hospitalRepo,
                                   ReminderRepository reminderRepo,
                                   IdGenerator idGenerator,
                                   AppProperties appProperties) {
        this.appointmentRepo = appointmentRepo;
        this.petRepo = petRepo;
        this.vetRepo = vetRepo;
        this.userAuthRepo = userAuthRepo;
        this.hospitalRepo = hospitalRepo;
        this.reminderRepo = reminderRepo;
        this.idGenerator = idGenerator;
        this.appProperties = appProperties;
    }

    @Transactional
    public OwnerAppointmentResponse bookAppointment(String ownerUserId, AppointmentBookingRequest request) {
        // 1. Validate pet belongs to owner and is active
        Pet pet = petRepo.findByPetIdAndOwnerUserIdAndDeletedAtIsNull(request.petId(), ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));

        // 2. Validate vet is verified and ACTIVE
        Vet vet = vetRepo.findById(request.vetUserId())
                .filter(v -> v != null && v.isVerified())
                .orElseThrow(() -> new BadRequestException("Vet not found or not verified"));

        UserAuth vetUser = userAuthRepo.findById(request.vetUserId())
                .filter(u -> "ACTIVE".equals(u.getStatus()))
                .orElseThrow(() -> new BadRequestException("Vet is not active"));

        // 3. Validate hospital belongs to vet and is active (if provided)
        Hospital hospital = null;
        if (request.hospitalId() != null && !request.hospitalId().isBlank()) {
            hospital = hospitalRepo.findByHospitalIdAndVetUserIdAndActiveTrue(request.hospitalId(), request.vetUserId())
                    .orElseThrow(() -> new BadRequestException("Hospital does not belong to this vet or is inactive"));
        }

        // 4. Validate timing, slot boundary, lead time, and opening hours
        validateSlotTiming(request.appointmentAt(), hospital);

        // 5. Validate pet has no other non-cancelled appointment at this time
        if (appointmentRepo.existsSlotForPet(request.petId(), request.appointmentAt())) {
            throw new ConflictException("Pet already has an active appointment at this time");
        }

        // 6. Check slot availability for vet
        if (appointmentRepo.existsSlotForVet(request.vetUserId(), request.appointmentAt())) {
            throw new ConflictException("Slot no longer available");
        }

        // 7. Persist appointment
        Appointment apt = new Appointment();
        apt.setAppointmentId(idGenerator.newId());
        apt.setPetId(request.petId());
        apt.setOwnerUserId(ownerUserId);
        apt.setVetUserId(request.vetUserId());
        apt.setHospitalId(hospital != null ? hospital.getHospitalId() : null);
        apt.setAppointmentAt(request.appointmentAt());
        apt.setStatus("BOOKED");
        apt.setReason(request.notes());

        try {
            apt = appointmentRepo.saveAndFlush(apt);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Concurrent booking conflict for vet {} at {}: {}", request.vetUserId(), request.appointmentAt(), ex.getMessage());
            throw new ConflictException("Slot no longer available");
        }

        // 8. Create TWO reminders (one for owner, one for vet) for each configured offset
        createRemindersForAppointment(apt, pet.getPetId(), pet.getName());

        log.info("Appointment booked: appointmentId={}, ownerUserId={}, vetUserId={}",
                apt.getAppointmentId(), ownerUserId, request.vetUserId());

        return toResponse(apt, vet, hospital, pet);
    }

    @Transactional(readOnly = true)
    public PageResponse<OwnerAppointmentResponse> listAppointments(
            String ownerUserId, String status, String petId,
            LocalDate from, LocalDate to,
            int page, int size, String sort, String direction) {

        int boundedSize = Math.min(Math.max(size, 1), 50);
        String sortField = ALLOWED_SORT_FIELDS.contains(sort) ? sort : "appointmentAt";
        Sort.Direction sortDir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(page, 0), boundedSize, Sort.by(sortDir, sortField));

        ZoneId zone = ZoneId.of(appProperties.zone());
        Instant fromInstant = from != null ? from.atStartOfDay(zone).toInstant() : null;
        Instant toInstant = to != null ? to.plusDays(1).atStartOfDay(zone).toInstant() : null;

        Page<Appointment> aptPage = appointmentRepo.findOwnerAppointments(
                ownerUserId, status, petId, fromInstant, toInstant, pageable);

        List<OwnerAppointmentResponse> content = aptPage.getContent().stream()
                .map(a -> toResponseFromEntity(a))
                .toList();

        return new PageResponse<>(content, aptPage.getNumber(), aptPage.getSize(),
                aptPage.getTotalElements(), aptPage.getTotalPages());
    }

    @Transactional(readOnly = true)
    public OwnerAppointmentResponse getAppointment(String ownerUserId, String appointmentId) {
        Appointment apt = appointmentRepo.findByAppointmentIdAndOwnerUserId(appointmentId, ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        return toResponseFromEntity(apt);
    }

    @Transactional
    public OwnerAppointmentResponse reschedule(String ownerUserId, String appointmentId, AppointmentRescheduleRequest request) {
        Appointment apt = appointmentRepo.findByAppointmentIdAndOwnerUserId(appointmentId, ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (!"BOOKED".equals(apt.getStatus()) && !"CONFIRMED".equals(apt.getStatus())) {
            throw new ConflictException("Cannot reschedule appointment with status " + apt.getStatus());
        }

        Hospital hospital = null;
        if (apt.getHospitalId() != null) {
            hospital = hospitalRepo.findById(apt.getHospitalId()).orElse(null);
        }

        validateSlotTiming(request.appointmentAt(), hospital);

        if (appointmentRepo.existsSlotForPet(apt.getPetId(), request.appointmentAt())) {
            throw new ConflictException("Pet already has an active appointment at this time");
        }
        if (appointmentRepo.existsSlotForVet(apt.getVetUserId(), request.appointmentAt())) {
            throw new ConflictException("Slot no longer available");
        }

        apt.setAppointmentAt(request.appointmentAt());
        apt.setStatus("BOOKED"); // Resets to BOOKED so vet confirms again

        try {
            apt = appointmentRepo.saveAndFlush(apt);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Slot no longer available");
        }

        // Delete old unsent reminders and recreate
        reminderRepo.deleteUnsentByAppointmentId(appointmentId);
        Pet pet = petRepo.findById(apt.getPetId()).orElse(null);
        createRemindersForAppointment(apt, apt.getPetId(), pet != null ? pet.getName() : "Pet");

        log.info("Appointment rescheduled: appointmentId={}, newTime={}", appointmentId, request.appointmentAt());
        return toResponseFromEntity(apt);
    }

    @Transactional
    public OwnerAppointmentResponse cancel(String ownerUserId, String appointmentId, AppointmentCancelRequest request) {
        Appointment apt = appointmentRepo.findByAppointmentIdAndOwnerUserId(appointmentId, ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (!"BOOKED".equals(apt.getStatus()) && !"CONFIRMED".equals(apt.getStatus())) {
            throw new ConflictException("Cannot cancel appointment with status " + apt.getStatus());
        }

        int cutoffHours = appProperties.appointment().cancelCutoffHours();
        Instant cutoffTime = apt.getAppointmentAt().minus(Duration.ofHours(cutoffHours));
        if (Instant.now().isAfter(cutoffTime)) {
            throw new ConflictException("Appointments cannot be cancelled within " + cutoffHours + " hours of scheduled time");
        }

        apt.setStatus("CANCELLED");
        if (request != null && request.reason() != null && !request.reason().isBlank()) {
            apt.setReason(request.reason());
        }
        apt = appointmentRepo.save(apt);

        // Delete unsent reminders
        reminderRepo.deleteUnsentByAppointmentId(appointmentId);

        log.info("Appointment cancelled by owner: appointmentId={}", appointmentId);
        return toResponseFromEntity(apt);
    }

    private void validateSlotTiming(Instant appointmentAt, Hospital hospital) {
        ZoneId zone = ZoneId.of(appProperties.zone());
        Instant now = Instant.now();

        // 1. Min lead time
        int minLeadMinutes = appProperties.appointment().minLeadMinutes();
        if (appointmentAt.isBefore(now.plus(Duration.ofMinutes(minLeadMinutes)))) {
            throw new BadRequestException("Appointment must be booked at least " + minLeadMinutes + " minutes in advance");
        }

        // 2. Slot boundary check
        int slotMinutes = appProperties.appointment().slotMinutes();
        ZonedDateTime zdt = ZonedDateTime.ofInstant(appointmentAt, zone);
        if (zdt.getSecond() != 0 || zdt.getNano() != 0 || (zdt.getMinute() % slotMinutes != 0)) {
            throw new BadRequestException("Appointment time must fall on a " + slotMinutes + "-minute slot boundary");
        }

        // 3. Opening hours check
        if (hospital != null && !hospital.isIs24x7()) {
            LocalTime open = hospital.getOpenTime();
            LocalTime close = hospital.getCloseTime();
            if (open != null && close != null) {
                LocalTime appTime = zdt.toLocalTime();
                if (open.isBefore(close)) {
                    // Normal shift
                    if (appTime.isBefore(open) || appTime.compareTo(close.minusMinutes(slotMinutes)) > 0) {
                        throw new BadRequestException("Appointment time is outside the hospital's opening hours (" + open + " - " + close + ")");
                    }
                } else {
                    // Overnight shift (e.g. 20:00 - 06:00)
                    boolean inside = !appTime.isBefore(open) || appTime.compareTo(close.minusMinutes(slotMinutes)) <= 0;
                    if (!inside) {
                        throw new BadRequestException("Appointment time is outside the hospital's opening hours (" + open + " - " + close + ")");
                    }
                }
            }
        }
    }

    private void createRemindersForAppointment(Appointment apt, String petId, String petName) {
        Instant aptTime = apt.getAppointmentAt();
        Instant now = Instant.now();
        List<Integer> offsets = appProperties.appointment().reminderOffsetsHours();

        for (int hours : offsets) {
            Instant remindTime = aptTime.minus(Duration.ofHours(hours));
            if (remindTime.isAfter(now)) {
                // Reminder for Owner
                Reminder ownerReminder = new Reminder();
                ownerReminder.setReminderId(idGenerator.newId());
                ownerReminder.setAppointmentId(apt.getAppointmentId());
                ownerReminder.setUserId(apt.getOwnerUserId());
                ownerReminder.setPetId(petId);
                ownerReminder.setRemindAt(remindTime);
                ownerReminder.setMessage("Upcoming appointment for " + petName + " in " + hours + " hours");
                reminderRepo.save(ownerReminder);

                // Reminder for Vet
                Reminder vetReminder = new Reminder();
                vetReminder.setReminderId(idGenerator.newId());
                vetReminder.setAppointmentId(apt.getAppointmentId());
                vetReminder.setUserId(apt.getVetUserId());
                vetReminder.setPetId(petId);
                vetReminder.setRemindAt(remindTime);
                vetReminder.setMessage("Upcoming appointment with " + petName + " in " + hours + " hours");
                reminderRepo.save(vetReminder);
            }
        }
    }

    private OwnerAppointmentResponse toResponseFromEntity(Appointment a) {
        Vet vet = vetRepo.findById(a.getVetUserId()).orElse(null);
        Hospital hospital = a.getHospitalId() != null ? hospitalRepo.findById(a.getHospitalId()).orElse(null) : null;
        Pet pet = a.getPet() != null ? a.getPet() : petRepo.findById(a.getPetId()).orElse(null);
        return toResponse(a, vet, hospital, pet);
    }

    private OwnerAppointmentResponse toResponse(Appointment a, Vet vet, Hospital h, Pet pet) {
        OwnerAppointmentResponse.VetSummary vetSummary = vet != null
                ? new OwnerAppointmentResponse.VetSummary(vet.getUserId(), vet.getFirstName(), vet.getLastName(), vet.getPhotoUrl())
                : null;

        OwnerAppointmentResponse.HospitalSummary hospitalSummary = h != null
                ? new OwnerAppointmentResponse.HospitalSummary(
                        h.getHospitalId(), h.getName(), h.getAddress1(), h.getAddress2(),
                        h.getCity(), h.getState(), h.getPincode(), h.getMobile())
                : null;

        OwnerAppointmentResponse.PetSummary petSummary = pet != null
                ? new OwnerAppointmentResponse.PetSummary(
                        pet.getPetId(), pet.getName(), pet.getSpecies(), pet.getBreed(),
                        pet.getGender(), pet.getDob(), pet.getPhotoUrl())
                : null;

        return new OwnerAppointmentResponse(
                a.getAppointmentId(),
                a.getAppointmentAt(),
                a.getStatus(),
                a.getReason(),
                a.getReason(), // notes
                a.getVisitNotes(),
                a.getVersion(),
                a.getCreatedAt(),
                a.getUpdatedAt(),
                vetSummary,
                hospitalSummary,
                petSummary
        );
    }
}
