package com.pet.vetalya.owner.service;

import com.pet.vetalya.entity.Appointment;
import com.pet.vetalya.entity.Hospital;
import com.pet.vetalya.entity.Pet;
import com.pet.vetalya.entity.Vet;
import com.pet.vetalya.owner.dto.OwnerAppointmentResponse;
import com.pet.vetalya.owner.dto.OwnerDashboardResponse;
import com.pet.vetalya.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class OwnerDashboardService {

    private final PetRepository petRepo;
    private final AppointmentRepository appointmentRepo;
    private final VaccinationRepository vaccinationRepo;
    private final VetRepository vetRepo;
    private final HospitalRepository hospitalRepo;

    public OwnerDashboardService(PetRepository petRepo,
                                 AppointmentRepository appointmentRepo,
                                 VaccinationRepository vaccinationRepo,
                                 VetRepository vetRepo,
                                 HospitalRepository hospitalRepo) {
        this.petRepo = petRepo;
        this.appointmentRepo = appointmentRepo;
        this.vaccinationRepo = vaccinationRepo;
        this.vetRepo = vetRepo;
        this.hospitalRepo = hospitalRepo;
    }

    @Transactional(readOnly = true)
    public OwnerDashboardResponse getDashboard(String ownerUserId) {
        // 1. Pet count
        long petCount = petRepo.countByOwnerUserIdAndDeletedAtIsNull(ownerUserId);

        // 2. Next 3 upcoming appointments
        List<Appointment> upcomingEntities = appointmentRepo.findNext3UpcomingForOwner(
                ownerUserId, Instant.now(), PageRequest.of(0, 3));
        List<OwnerAppointmentResponse> upcomingAppointments = upcomingEntities.stream()
                .map(a -> toResponse(a))
                .toList();

        // 3. Count of pending (BOOKED) appointments
        long pendingCount = appointmentRepo.countPendingForOwner(ownerUserId);

        // 4. Vaccinations due in next 30 days
        LocalDate today = LocalDate.now();
        long vaxDueCount = vaccinationRepo.countDueForOwner(ownerUserId, today, today.plusDays(30));

        // 5. Reviewable appointments count (COMPLETED without a review)
        long reviewableCount = appointmentRepo.countUnreviewedCompletedAppointments(ownerUserId);

        return new OwnerDashboardResponse(
                petCount,
                upcomingAppointments,
                pendingCount,
                vaxDueCount,
                reviewableCount
        );
    }

    private OwnerAppointmentResponse toResponse(Appointment a) {
        Vet vet = vetRepo.findById(a.getVetUserId()).orElse(null);
        Hospital h = a.getHospitalId() != null ? hospitalRepo.findById(a.getHospitalId()).orElse(null) : null;
        Pet pet = a.getPet() != null ? a.getPet() : petRepo.findById(a.getPetId()).orElse(null);

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
                a.getReason(),
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
