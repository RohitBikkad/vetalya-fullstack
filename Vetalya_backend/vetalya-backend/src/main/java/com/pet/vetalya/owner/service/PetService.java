package com.pet.vetalya.owner.service;

import com.pet.vetalya.common.GlobalExceptionHandler.ConflictException;
import com.pet.vetalya.common.GlobalExceptionHandler.ResourceNotFoundException;
import com.pet.vetalya.common.IdGenerator;
import com.pet.vetalya.common.PageResponse;
import com.pet.vetalya.config.AppProperties;
import com.pet.vetalya.entity.Appointment;
import com.pet.vetalya.entity.Pet;
import com.pet.vetalya.owner.dto.PetDetailResponse;
import com.pet.vetalya.owner.dto.PetRequest;
import com.pet.vetalya.owner.dto.PetResponse;
import com.pet.vetalya.repository.AppointmentRepository;
import com.pet.vetalya.repository.PetRepository;
import com.pet.vetalya.repository.VaccinationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Set;

@Service
public class PetService {

    private static final Logger log = LoggerFactory.getLogger(PetService.class);
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "createdAt", "dob");

    private final PetRepository petRepo;
    private final AppointmentRepository appointmentRepo;
    private final VaccinationRepository vaccinationRepo;
    private final IdGenerator idGenerator;
    private final AppProperties appProperties;

    public PetService(PetRepository petRepo,
                      AppointmentRepository appointmentRepo,
                      VaccinationRepository vaccinationRepo,
                      IdGenerator idGenerator,
                      AppProperties appProperties) {
        this.petRepo = petRepo;
        this.appointmentRepo = appointmentRepo;
        this.vaccinationRepo = vaccinationRepo;
        this.idGenerator = idGenerator;
        this.appProperties = appProperties;
    }

    @Transactional
    public PetResponse createPet(String ownerUserId, PetRequest request) {
        int maxPets = appProperties.pet().maxPerOwner();
        if (petRepo.countByOwnerUserIdAndDeletedAtIsNull(ownerUserId) >= maxPets) {
            throw new ConflictException("Maximum pets limit of " + maxPets + " reached for owner");
        }

        String regNo = request.registrationNo();
        if (regNo != null && !regNo.isBlank()) {
            if (petRepo.existsByRegistrationNoAndDeletedAtIsNull(regNo)) {
                throw new ConflictException("Pet with registration number '" + regNo + "' already exists");
            }
        } else {
            regNo = generateUniqueRegistrationNo();
        }

        Pet pet = new Pet();
        pet.setPetId(idGenerator.newId());
        pet.setOwnerUserId(ownerUserId);
        pet.setName(request.name());
        pet.setSpecies(request.species());
        pet.setGender(request.gender());
        pet.setDob(request.dob());
        pet.setBreed(request.breed());
        pet.setPhotoUrl(request.photoUrl());
        pet.setRegistrationNo(regNo);

        pet = petRepo.save(pet);
        log.info("Pet created: petId={}, ownerUserId={}", pet.getPetId(), ownerUserId);
        return toResponse(pet);
    }

    @Transactional(readOnly = true)
    public PageResponse<PetResponse> listPets(String ownerUserId, int page, int size, String sort, String direction) {
        int boundedSize = Math.min(Math.max(size, 1), 50);
        String sortField = ALLOWED_SORT_FIELDS.contains(sort) ? sort : "createdAt";
        Sort.Direction sortDir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(page, 0), boundedSize, Sort.by(sortDir, sortField));

        Page<Pet> petPage = petRepo.findAllByOwnerUserIdAndDeletedAtIsNull(ownerUserId, pageable);
        List<PetResponse> content = petPage.getContent().stream().map(this::toResponse).toList();
        return new PageResponse<>(content, petPage.getNumber(), petPage.getSize(),
                petPage.getTotalElements(), petPage.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PetDetailResponse getPet(String ownerUserId, String petId) {
        Pet pet = findOwnOrThrow(ownerUserId, petId);

        // Fetch next vaccination due
        List<LocalDate> nextVaxList = vaccinationRepo.findNextVaccinationDateForPet(
                petId, LocalDate.now(), PageRequest.of(0, 1));
        LocalDate nextVaccination = nextVaxList.isEmpty() ? null : nextVaxList.get(0);

        // Fetch next appointment
        List<Appointment> nextAptList = appointmentRepo.findNextAppointmentForPet(
                petId, ownerUserId, Instant.now(), PageRequest.of(0, 1));
        Instant nextAppointment = nextAptList.isEmpty() ? null : nextAptList.get(0).getAppointmentAt();

        return new PetDetailResponse(
                pet.getPetId(),
                pet.getName(),
                pet.getSpecies(),
                pet.getGender(),
                pet.getDob(),
                calculateAge(pet.getDob()),
                pet.getBreed(),
                pet.getPhotoUrl(),
                pet.getRegistrationNo(),
                nextVaccination,
                nextAppointment,
                pet.getCreatedAt(),
                pet.getUpdatedAt()
        );
    }

    @Transactional
    public PetResponse updatePet(String ownerUserId, String petId, PetRequest request) {
        Pet pet = findOwnOrThrow(ownerUserId, petId);

        pet.setName(request.name());
        pet.setSpecies(request.species());
        pet.setGender(request.gender());
        pet.setDob(request.dob());
        pet.setBreed(request.breed());
        pet.setPhotoUrl(request.photoUrl());
        // Registration number cannot be changed once set

        pet = petRepo.save(pet);
        log.info("Pet updated: petId={}, ownerUserId={}", petId, ownerUserId);
        return toResponse(pet);
    }

    @Transactional
    public void deletePet(String ownerUserId, String petId) {
        Pet pet = findOwnOrThrow(ownerUserId, petId);

        if (appointmentRepo.existsFutureBookedOrConfirmedForPet(petId, Instant.now())) {
            throw new ConflictException(
                    "Cannot delete pet with future appointments; please cancel them first");
        }

        pet.setDeletedAt(Instant.now());
        petRepo.save(pet);
        log.info("Pet soft-deleted: petId={}, ownerUserId={}", petId, ownerUserId);
    }

    private Pet findOwnOrThrow(String ownerUserId, String petId) {
        return petRepo.findByPetIdAndOwnerUserIdAndDeletedAtIsNull(petId, ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));
    }

    private String generateUniqueRegistrationNo() {
        int year = LocalDate.now().getYear();
        for (int i = 0; i < 5; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < 6; j++) {
                sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
            }
            String candidate = "PET-" + year + "-" + sb;
            if (!petRepo.existsByRegistrationNoAndDeletedAtIsNull(candidate)) {
                return candidate;
            }
        }
        throw new ConflictException("Failed to generate unique registration number, please provide one");
    }

    private String calculateAge(LocalDate dob) {
        if (dob == null) return "N/A";
        Period p = Period.between(dob, LocalDate.now());
        if (p.getYears() > 0) {
            return p.getYears() + " year" + (p.getYears() > 1 ? "s" : "")
                    + (p.getMonths() > 0 ? " " + p.getMonths() + " month" + (p.getMonths() > 1 ? "s" : "") : "");
        } else if (p.getMonths() > 0) {
            return p.getMonths() + " month" + (p.getMonths() > 1 ? "s" : "");
        } else {
            return p.getDays() + " day" + (p.getDays() > 1 ? "s" : "");
        }
    }

    private PetResponse toResponse(Pet p) {
        return new PetResponse(
                p.getPetId(),
                p.getName(),
                p.getSpecies(),
                p.getGender(),
                p.getDob(),
                calculateAge(p.getDob()),
                p.getBreed(),
                p.getPhotoUrl(),
                p.getRegistrationNo(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
