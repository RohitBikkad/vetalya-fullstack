package com.pet.vetalya.owner.service;

import com.pet.vetalya.common.GlobalExceptionHandler.ResourceNotFoundException;
import com.pet.vetalya.config.AppProperties;
import com.pet.vetalya.entity.Hospital;
import com.pet.vetalya.entity.UserAuth;
import com.pet.vetalya.entity.Vet;
import com.pet.vetalya.owner.dto.SlotResponse;
import com.pet.vetalya.repository.AppointmentRepository;
import com.pet.vetalya.repository.HospitalRepository;
import com.pet.vetalya.repository.UserAuthRepository;
import com.pet.vetalya.repository.VetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AvailabilityService {

    private final VetRepository vetRepo;
    private final UserAuthRepository userAuthRepo;
    private final HospitalRepository hospitalRepo;
    private final AppointmentRepository appointmentRepo;
    private final AppProperties appProperties;

    public AvailabilityService(VetRepository vetRepo,
                               UserAuthRepository userAuthRepo,
                               HospitalRepository hospitalRepo,
                               AppointmentRepository appointmentRepo,
                               AppProperties appProperties) {
        this.vetRepo = vetRepo;
        this.userAuthRepo = userAuthRepo;
        this.hospitalRepo = hospitalRepo;
        this.appointmentRepo = appointmentRepo;
        this.appProperties = appProperties;
    }

    @Transactional(readOnly = true)
    public List<SlotResponse> getFreeSlots(String vetUserId, LocalDate date, String hospitalId) {
        Vet vet = vetRepo.findById(vetUserId)
                .filter(v -> v != null && v.isVerified())
                .orElseThrow(() -> new ResourceNotFoundException("Vet not found or not verified"));

        UserAuth user = userAuthRepo.findById(vetUserId)
                .filter(u -> "ACTIVE".equals(u.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("Vet not found or not active"));

        Hospital hospital = hospitalRepo.findByHospitalIdAndVetUserIdAndActiveTrue(hospitalId, vetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital not found for this vet"));

        ZoneId zone = ZoneId.of(appProperties.zone());
        int slotMinutes = appProperties.appointment().slotMinutes();
        Instant now = Instant.now();

        // Compute start and end instants for the operating hours on that date
        List<InstantRange> operatingRanges = computeOperatingRanges(date, hospital, zone);
        if (operatingRanges.isEmpty()) {
            return List.of();
        }

        // Find booked slots across the full span of operating ranges
        Instant queryStart = operatingRanges.get(0).start();
        Instant queryEnd = operatingRanges.get(operatingRanges.size() - 1).end();
        Set<Instant> bookedSlots = appointmentRepo.findBookedSlotsForVet(vetUserId, queryStart, queryEnd).stream()
                .collect(Collectors.toSet());

        List<SlotResponse> freeSlots = new ArrayList<>();
        for (InstantRange range : operatingRanges) {
            Instant slotStart = range.start();
            while (slotStart.plus(Duration.ofMinutes(slotMinutes)).compareTo(range.end()) <= 0) {
                Instant slotEnd = slotStart.plus(Duration.ofMinutes(slotMinutes));
                // Must be in the future and not booked
                if (slotStart.isAfter(now) && !bookedSlots.contains(slotStart)) {
                    freeSlots.add(new SlotResponse(slotStart, slotEnd, true));
                }
                slotStart = slotEnd;
            }
        }

        return freeSlots;
    }

    private List<InstantRange> computeOperatingRanges(LocalDate date, Hospital h, ZoneId zone) {
        if (h.isIs24x7()) {
            Instant start = date.atStartOfDay(zone).toInstant();
            Instant end = date.plusDays(1).atStartOfDay(zone).toInstant();
            return List.of(new InstantRange(start, end));
        }

        LocalTime openTime = h.getOpenTime();
        LocalTime closeTime = h.getCloseTime();
        if (openTime == null || closeTime == null) {
            return List.of();
        }

        if (openTime.isBefore(closeTime)) {
            // Same-day operating hours (e.g. 09:00 to 18:00)
            Instant start = date.atTime(openTime).atZone(zone).toInstant();
            Instant end = date.atTime(closeTime).atZone(zone).toInstant();
            return List.of(new InstantRange(start, end));
        } else {
            // Overnight shift (e.g. 20:00 to 06:00 next morning)
            Instant start = date.atTime(openTime).atZone(zone).toInstant();
            Instant end = date.plusDays(1).atTime(closeTime).atZone(zone).toInstant();
            return List.of(new InstantRange(start, end));
        }
    }

    public record InstantRange(Instant start, Instant end) {}
}
