package com.pet.vetalya.vet.service;

import com.pet.vetalya.common.GlobalExceptionHandler.*;
import com.pet.vetalya.common.IdGenerator;
import com.pet.vetalya.entity.Vaccination;
import com.pet.vetalya.repository.AppointmentRepository;
import com.pet.vetalya.repository.PetRepository;
import com.pet.vetalya.repository.VaccinationRepository;
import com.pet.vetalya.vet.dto.VaccinationRequest;
import com.pet.vetalya.vet.dto.VaccinationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class VaccinationService {

    private static final Logger log = LoggerFactory.getLogger(VaccinationService.class);

    private final VaccinationRepository vaccinationRepo;
    private final AppointmentRepository appointmentRepo;
    private final PetRepository petRepo;
    private final IdGenerator idGenerator;

    public VaccinationService(VaccinationRepository vaccinationRepo,
                              AppointmentRepository appointmentRepo,
                              PetRepository petRepo,
                              IdGenerator idGenerator) {
        this.vaccinationRepo = vaccinationRepo;
        this.appointmentRepo = appointmentRepo;
        this.petRepo = petRepo;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public VaccinationResponse create(String vetUserId, String petId, VaccinationRequest request) {
        validatePetExists(petId);
        validateVetHasAppointmentForPet(vetUserId, petId);
        validateDates(request);

        Vaccination v = new Vaccination();
        v.setVaccinationId(idGenerator.newId());
        v.setPetId(petId);
        v.setVetUserId(vetUserId);
        applyFields(v, request);

        v = vaccinationRepo.save(v);
        log.info("Vaccination recorded: vaccinationId={}, petId={}, vetUserId={}", v.getVaccinationId(), petId, vetUserId);
        return toResponse(v);
    }

    @Transactional(readOnly = true)
    public List<VaccinationResponse> listForPet(String vetUserId, String petId) {
        validatePetExists(petId);
        validateVetHasAppointmentForPet(vetUserId, petId);
        return vaccinationRepo.findAllByPetIdAndVetUserId(petId, vetUserId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public VaccinationResponse update(String vetUserId, String vaccinationId, VaccinationRequest request) {
        validateDates(request);
        Vaccination v = findCreatedByVetOrThrow(vetUserId, vaccinationId);
        applyFields(v, request);
        v = vaccinationRepo.save(v);
        log.info("Vaccination updated: vaccinationId={}", vaccinationId);
        return toResponse(v);
    }

    @Transactional
    public void delete(String vetUserId, String vaccinationId) {
        Vaccination v = findCreatedByVetOrThrow(vetUserId, vaccinationId);
        vaccinationRepo.delete(v);
        log.info("Vaccination deleted: vaccinationId={}", vaccinationId);
    }

    @Transactional(readOnly = true)
    public List<VaccinationResponse> findDue(String vetUserId, LocalDate from, LocalDate to) {
        return vaccinationRepo.findDueForVet(vetUserId, from, to).stream()
                .map(this::toResponse)
                .toList();
    }

    private void validatePetExists(String petId) {
        if (!petRepo.existsById(petId)) {
            throw new ResourceNotFoundException("Pet not found");
        }
    }

    private void validateVetHasAppointmentForPet(String vetUserId, String petId) {
        if (!appointmentRepo.existsNonCancelledForVetAndPet(vetUserId, petId)) {
            throw new ForbiddenException(
                    "Vet must have at least one non-cancelled appointment for this pet");
        }
    }

    private void validateDates(VaccinationRequest request) {
        if (request.lastVaccinationDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Last vaccination date cannot be in the future");
        }
        if (request.nextVaccinationDate() != null
                && !request.nextVaccinationDate().isAfter(request.lastVaccinationDate())) {
            throw new BadRequestException(
                    "Next vaccination date must be after the last vaccination date");
        }
    }

    private Vaccination findCreatedByVetOrThrow(String vetUserId, String vaccinationId) {
        return vaccinationRepo.findByVaccinationIdAndVetUserId(vaccinationId, vetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination not found"));
    }

    private void applyFields(Vaccination v, VaccinationRequest r) {
        v.setVaccineName(r.vaccineName());
        v.setBatchNumber(r.batchNumber());
        v.setLastVaccinationDate(r.lastVaccinationDate());
        v.setNextVaccinationDate(r.nextVaccinationDate());
        v.setNotes(r.notes());
    }

    private VaccinationResponse toResponse(Vaccination v) {
        return new VaccinationResponse(
                v.getVaccinationId(),
                v.getPetId(),
                v.getVetUserId(),
                v.getVaccineName(),
                v.getBatchNumber(),
                v.getLastVaccinationDate(),
                v.getNextVaccinationDate(),
                v.getNotes(),
                v.getCreatedAt(),
                v.getUpdatedAt()
        );
    }
}
