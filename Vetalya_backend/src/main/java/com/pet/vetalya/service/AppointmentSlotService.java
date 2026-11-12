package com.pet.vetalya.service;

import com.pet.vetalya.common.GlobalExceptionHandler.ConflictException;
import com.pet.vetalya.common.GlobalExceptionHandler.ResourceNotFoundException;
import com.pet.vetalya.common.IdGenerator;
import com.pet.vetalya.dto.BookSlotRequest;
import com.pet.vetalya.dto.GenerateSlotsRequest;
import com.pet.vetalya.dto.SlotResponse;
import com.pet.vetalya.entity.Appointment;
import com.pet.vetalya.entity.AppointmentSlot;
import com.pet.vetalya.entity.Pet;
import com.pet.vetalya.repository.AppointmentRepository;
import com.pet.vetalya.repository.AppointmentSlotRepository;
import com.pet.vetalya.repository.PetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class AppointmentSlotService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentSlotService.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final AppointmentSlotRepository slotRepo;
    private final AppointmentRepository appointmentRepo;
    private final PetRepository petRepo;
    private final IdGenerator idGenerator;

    public AppointmentSlotService(AppointmentSlotRepository slotRepo,
                                  AppointmentRepository appointmentRepo,
                                  PetRepository petRepo,
                                  IdGenerator idGenerator) {
        this.slotRepo = slotRepo;
        this.appointmentRepo = appointmentRepo;
        this.petRepo = petRepo;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public List<SlotResponse> generateSlots(String vetUserId, GenerateSlotsRequest req) {
        LocalDate fromDate = req.fromDate() != null ? req.fromDate() : LocalDate.now(IST);
        LocalDate toDate = req.toDate() != null ? req.toDate() : fromDate.plusDays(7);
        LocalTime startTime = req.startTime() != null ? req.startTime() : LocalTime.of(9, 0);
        LocalTime endTime = req.endTime() != null ? req.endTime() : LocalTime.of(17, 0);
        int durationMinutes = req.slotDurationMinutes() != null && req.slotDurationMinutes() > 0 ? req.slotDurationMinutes() : 20;
        String slotType = req.slotType() != null ? req.slotType() : "OPD_CONSULTATION";
        int capacity = req.capacity() != null && req.capacity() > 0 ? req.capacity() : 1;

        List<AppointmentSlot> createdSlots = new ArrayList<>();

        for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
            LocalTime current = startTime;
            while (current.plusMinutes(durationMinutes).isBefore(endTime) || current.plusMinutes(durationMinutes).equals(endTime)) {
                LocalTime next = current.plusMinutes(durationMinutes);
                if (slotRepo.findByVetUserIdAndSlotDateAndStartTime(vetUserId, date, current).isEmpty()) {
                    AppointmentSlot slot = new AppointmentSlot();
                    slot.setSlotId(idGenerator.newId());
                    slot.setVetUserId(vetUserId);
                    slot.setHospitalId(req.hospitalId());
                    slot.setSlotDate(date);
                    slot.setStartTime(current);
                    slot.setEndTime(next);
                    slot.setSlotType(slotType);
                    slot.setCapacity(capacity);
                    slot.setBookedCount(0);
                    slot.setStatus("AVAILABLE");
                    createdSlots.add(slotRepo.save(slot));
                }
                current = next;
            }
        }

        return createdSlots.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SlotResponse> getAvailableSlots(String vetUserId, LocalDate fromDate, LocalDate toDate) {
        LocalDate start = fromDate != null ? fromDate : LocalDate.now(IST);
        LocalDate end = toDate != null ? toDate : start.plusDays(14);
        return slotRepo.findAvailableSlots(vetUserId, start, end).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public Appointment bookSlotAtomically(String ownerUserId, BookSlotRequest req) {
        // 1. Pessimistic write lock on slot to ensure zero double-booking under concurrency
        AppointmentSlot slot = slotRepo.findByIdWithPessimisticLock(req.slotId())
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found: " + req.slotId()));

        if (!"AVAILABLE".equals(slot.getStatus()) || slot.getBookedCount() >= slot.getCapacity()) {
            throw new ConflictException("Slot is no longer available. Please select another slot.");
        }

        // 2. Validate pet exists and belongs to owner
        Pet pet = petRepo.findById(req.petId())
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found: " + req.petId()));

        if (!ownerUserId.equals(pet.getOwnerUserId())) {
            throw new ConflictException("Pet does not belong to the current authenticated owner");
        }

        // 3. Update slot capacity count
        slot.setBookedCount(slot.getBookedCount() + 1);
        if (slot.getBookedCount() >= slot.getCapacity()) {
            slot.setStatus("BOOKED");
        }
        slotRepo.save(slot);

        // 4. Calculate appointment instant and queue number
        LocalDateTime slotDateTime = LocalDateTime.of(slot.getSlotDate(), slot.getStartTime());
        Instant appointmentAt = slotDateTime.atZone(IST).toInstant();
        int queueNumber = slot.getBookedCount();

        // 5. Create Appointment
        Appointment apt = new Appointment();
        apt.setAppointmentId(idGenerator.newId());
        apt.setSlotId(slot.getSlotId());
        apt.setPetId(pet.getPetId());
        apt.setOwnerUserId(ownerUserId);
        apt.setVetUserId(slot.getVetUserId());
        apt.setHospitalId(slot.getHospitalId());
        apt.setAppointmentAt(appointmentAt);
        apt.setStatus("BOOKED");
        apt.setReason(req.reason() != null ? req.reason() : "Consultation");
        apt.setQueueNumber(queueNumber);
        apt.setAppointmentType(req.appointmentType() != null ? req.appointmentType() : slot.getSlotType());

        Appointment saved = appointmentRepo.save(apt);
        log.info("Pessimistically locked slot {} booked successfully for pet {} queue #{}", slot.getSlotId(), pet.getPetId(), queueNumber);
        return saved;
    }

    @Transactional
    public void checkInAppointment(String appointmentId) {
        Appointment apt = appointmentRepo.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));
        apt.setCheckInTime(Instant.now());
        apt.setStatus("CONFIRMED");
        appointmentRepo.save(apt);
    }

    @Transactional
    public void startConsultation(String appointmentId) {
        Appointment apt = appointmentRepo.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));
        apt.setConsultationStartTime(Instant.now());
        appointmentRepo.save(apt);
    }

    @Transactional
    public void endConsultation(String appointmentId) {
        Appointment apt = appointmentRepo.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));
        apt.setConsultationEndTime(Instant.now());
        apt.setStatus("COMPLETED");
        appointmentRepo.save(apt);
    }

    private SlotResponse mapToResponse(AppointmentSlot slot) {
        return new SlotResponse(
                slot.getSlotId(),
                slot.getHospitalId(),
                slot.getVetUserId(),
                slot.getSlotDate(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getSlotType(),
                slot.getCapacity(),
                slot.getBookedCount(),
                slot.getStatus()
        );
    }
}

