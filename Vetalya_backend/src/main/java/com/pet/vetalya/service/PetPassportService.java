package com.pet.vetalya.service;

import com.pet.vetalya.common.GlobalExceptionHandler.ConflictException;
import com.pet.vetalya.common.GlobalExceptionHandler.ForbiddenException;
import com.pet.vetalya.common.GlobalExceptionHandler.ResourceNotFoundException;
import com.pet.vetalya.common.IdGenerator;
import com.pet.vetalya.clinic.ClinicalAccess;
import com.pet.vetalya.dto.*;
import com.pet.vetalya.entity.*;
import com.pet.vetalya.repository.*;
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
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class PetPassportService {

    private static final Logger log = LoggerFactory.getLogger(PetPassportService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String HEX = "0123456789abcdef";

    private final PetRepository petRepo;
    private final PetHealthPassportRepository passportRepo;
    private final PetIdentifierRepository identifierRepo;
    private final RecordShareRequestRepository shareRepo;
    private final PetDocumentRepository documentRepo;
    private final VaccinationRepository vaccinationRepo;
    private final AppointmentRepository appointmentRepo;
    private final IdGenerator idGenerator;
    private final ClinicalAccess clinicalAccess;

    private enum SharingScope { FULL_TIMELINE, VACCINES_ONLY, EMERGENCY_SUMMARY }

    public PetPassportService(PetRepository petRepo,
                              PetHealthPassportRepository passportRepo,
                              PetIdentifierRepository identifierRepo,
                              RecordShareRequestRepository shareRepo,
                              PetDocumentRepository documentRepo,
                              VaccinationRepository vaccinationRepo,
                              AppointmentRepository appointmentRepo,
                              IdGenerator idGenerator, ClinicalAccess clinicalAccess) {
        this.petRepo = petRepo;
        this.passportRepo = passportRepo;
        this.identifierRepo = identifierRepo;
        this.shareRepo = shareRepo;
        this.documentRepo = documentRepo;
        this.vaccinationRepo = vaccinationRepo;
        this.appointmentRepo = appointmentRepo;
        this.idGenerator = idGenerator;
        this.clinicalAccess = clinicalAccess;
    }

    /**
     * Get or create a Pet Health Passport.
     */
    @Transactional
    public PetPassportResponse getOrCreatePassport(String currentUserId, String petId, String hospitalId) {
        Pet pet = petRepo.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found with ID: " + petId));

        if (!pet.getOwnerUserId().equals(currentUserId)) {
            var scopes = sharingScopes(currentUserId, pet, hospitalId);
            if (!scopes.contains(SharingScope.FULL_TIMELINE) && !scopes.contains(SharingScope.EMERGENCY_SUMMARY))
                throw consentDenied();
            var passport = passportRepo.findByPetId(petId)
                    .orElseThrow(() -> new ResourceNotFoundException("Pet passport not found"));
            return sharedSummary(pet, passport, scopes.contains(SharingScope.FULL_TIMELINE));
        }

        PetHealthPassport passport = passportRepo.findByPetId(petId).orElseGet(() -> {
            PetHealthPassport newPassport = new PetHealthPassport();
            newPassport.setPassportId(idGenerator.newId());
            newPassport.setPetId(pet.getPetId());

            String pNum = pet.getPassportNumber();
            if (pNum == null || pNum.isBlank()) {
                pNum = "VET-" + LocalDate.now().getYear() + "-" + pet.getPetId().substring(Math.max(0, pet.getPetId().length() - 6)).toUpperCase();
                pet.setPassportNumber(pNum);
                petRepo.save(pet);
            }
            newPassport.setPassportNumber(pNum);
            newPassport.setPublicEmergencyQrHash(generateQrHash());
            return passportRepo.save(newPassport);
        });

        return toPassportResponse(pet, passport);
    }

    /**
     * Update permitted passport fields.
     */
    @Transactional
    public PetPassportResponse updatePassport(String currentUserId, String petId, PetPassportUpdateRequest req) {
        Pet pet = petRepo.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found with ID: " + petId));

        if (!pet.getOwnerUserId().equals(currentUserId)) {
            throw new ForbiddenException("Only the pet's primary owner can update the passport");
        }

        PetHealthPassport passport = passportRepo.findByPetId(petId)
                .orElseGet(() -> {
                    PetHealthPassport p = new PetHealthPassport();
                    p.setPassportId(idGenerator.newId());
                    p.setPetId(pet.getPetId());
                    p.setPassportNumber(pet.getPassportNumber() != null ? pet.getPassportNumber() : "VET-" + pet.getPetId());
                    p.setPublicEmergencyQrHash(generateQrHash());
                    return p;
                });

        if (req.bloodGroup() != null) passport.setBloodGroup(req.bloodGroup());
        if (req.allergiesSummary() != null) passport.setAllergiesSummary(req.allergiesSummary());
        if (req.chronicConditionsSummary() != null) passport.setChronicConditionsSummary(req.chronicConditionsSummary());
        if (req.dietaryNotes() != null) passport.setDietaryNotes(req.dietaryNotes());
        if (req.emergencyContactPhone() != null) passport.setEmergencyContactPhone(req.emergencyContactPhone());

        if (req.weightKg() != null) pet.setWeightKg(req.weightKg());
        if (req.color() != null) pet.setColor(req.color());
        if (req.distinctiveMarks() != null) pet.setDistinctiveMarks(req.distinctiveMarks());
        if (req.microchipNumber() != null) pet.setMicrochipNumber(req.microchipNumber());

        petRepo.save(pet);
        passport = passportRepo.save(passport);

        return toPassportResponse(pet, passport);
    }

    /**
     * Get emergency public overview via unguessable collar QR token.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getEmergencyProfileByQr(String qrHash) {
        PetHealthPassport passport = passportRepo.findByPublicEmergencyQrHashAndQrRevokedIsFalse(qrHash)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or revoked emergency QR code"));

        Pet pet = petRepo.findById(passport.getPetId())
                .orElseThrow(() -> new ResourceNotFoundException("Pet record not found"));

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", pet.getName());
        map.put("species", pet.getSpecies());
        map.put("breed", pet.getBreed());
        map.put("gender", pet.getGender());
        map.put("photoUrl", pet.getPhotoUrl());
        map.put("passportNumber", passport.getPassportNumber());
        map.put("bloodGroup", passport.getBloodGroup());
        map.put("criticalAllergies", passport.getAllergiesSummary());
        map.put("chronicConditions", passport.getChronicConditionsSummary());
        map.put("emergencyContactPhone", passport.getEmergencyContactPhone());
        return map;
    }

    /**
     * Get unified medical timeline.
     */
    @Transactional(readOnly = true)
    public List<PetTimelineItemDto> getPetTimeline(String currentUserId, String petId, String hospitalId) {
        Pet pet = petRepo.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found with ID: " + petId));

        boolean owner = pet.getOwnerUserId().equals(currentUserId);
        var scopes = owner ? EnumSet.of(SharingScope.FULL_TIMELINE) : sharingScopes(currentUserId, pet, hospitalId);
        boolean fullTimeline = scopes.contains(SharingScope.FULL_TIMELINE);
        if (!fullTimeline && !scopes.contains(SharingScope.VACCINES_ONLY)) throw consentDenied();

        List<PetTimelineItemDto> timeline = new ArrayList<>();

        // 1. Vaccinations
        List<Vaccination> vaccinations = vaccinationRepo.findAllByPetIdOrderByLastVaccinationDateDesc(petId);
        for (Vaccination v : vaccinations) {
            timeline.add(new PetTimelineItemDto(
                    v.getVaccinationId(),
                    "VACCINATION",
                    v.getVaccineName(),
                    "Batch: " + (v.getBatchNumber() != null ? v.getBatchNumber() : "N/A"),
                    v.getNotes() != null ? v.getNotes() : "Vaccination administered",
                    "Attending Veterinarian",
                    "VERIFIED_BY_VET",
                    null,
                    v.getCreatedAt()
            ));
        }

        // 2. Encounters & Completed Consultations
        List<Appointment> completed = fullTimeline
                ? appointmentRepo.findAllByPetIdAndStatus(petId, "COMPLETED") : List.of();
        for (Appointment apt : completed) {
            String desc = apt.getReason() != null ? apt.getReason() : "Clinical Consultation";
            timeline.add(new PetTimelineItemDto(
                    apt.getAppointmentId(),
                    "CONSULTATION",
                    "Clinical Encounter",
                    apt.getHospital() != null ? apt.getHospital().getName() : "Vetalya Clinic",
                    desc,
                    apt.getVet() != null ? "Dr. " + apt.getVet().getFirstName() + " " + apt.getVet().getLastName() : "Staff Vet",
                    "VERIFIED_BY_VET",
                    null,
                    apt.getAppointmentAt()
            ));
        }

        // 3. Uploaded Documents
        List<PetDocument> docs = fullTimeline
                ? documentRepo.findAllByPetIdAndIsDeletedIsFalse(petId) : List.of();
        for (PetDocument doc : docs) {
            timeline.add(new PetTimelineItemDto(
                    doc.getDocumentId(),
                    "DOCUMENT_UPLOAD",
                    doc.getCategory(),
                    doc.getFileName(),
                    doc.getDescription() != null ? doc.getDescription() : "Document attached to passport",
                    "Owner Upload",
                    doc.getVerificationStatus(),
                    owner ? doc.getStoragePath() : null,
                    doc.getCreatedAt()
            ));
        }

        // Sort descending by event timestamp
        timeline.sort((a, b) -> b.eventTimestamp().compareTo(a.eventTimestamp()));
        return timeline;
    }

    @Transactional(readOnly = true)
    public List<RecordShareRequestDto.Response> listShareRequests(String currentUserId, String petId) {
        Pet pet = petRepo.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));
        if (!pet.getOwnerUserId().equals(currentUserId)) {
            throw new ForbiddenException("Only the pet owner can view sharing history");
        }
        return shareRepo.findAllByPetId(petId).stream()
                .sorted(Comparator.comparing(RecordShareRequest::getCreatedAt).reversed())
                .map(share -> toShareResponse(share, pet.getName())).toList();
    }

    /**
     * Record Sharing: Request sharing.
     */
    @Transactional
    public RecordShareRequestDto.Response createShareRequest(String currentUserId, RecordShareRequestDto.CreateRequest req) {
        Pet pet = petRepo.findById(req.petId())
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found with ID: " + req.petId()));

        RecordShareRequest share = new RecordShareRequest();
        share.setRequestId(idGenerator.newId());
        share.setPetId(pet.getPetId());
        share.setRequesterUserId(currentUserId);
        share.setTargetHospitalId(req.targetHospitalId());
        share.setScope(req.scope() != null ? req.scope() : "FULL_TIMELINE");

        int hours = req.expiryHours() != null && req.expiryHours() > 0 ? req.expiryHours() : 72;
        share.setExpiresAt(Instant.now().plus(hours, ChronoUnit.HOURS));

        // If the pet owner initiated the share, auto-approve
        if (pet.getOwnerUserId().equals(currentUserId)) {
            share.setStatus("APPROVED");
            share.setApprovedAt(Instant.now());
        } else {
            share.setStatus("PENDING");
        }

        share = shareRepo.save(share);
        return toShareResponse(share, pet.getName());
    }

    /**
     * Record Sharing: Approve share request.
     */
    @Transactional
    public RecordShareRequestDto.Response approveShareRequest(String currentUserId, String requestId) {
        RecordShareRequest share = shareRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Share request not found"));

        Pet pet = petRepo.findById(share.getPetId())
                .orElseThrow(() -> new ResourceNotFoundException("Pet record not found"));

        if (!pet.getOwnerUserId().equals(currentUserId)) {
            throw new ForbiddenException("Only the pet owner can approve record sharing");
        }

        share.setStatus("APPROVED");
        share.setApprovedAt(Instant.now());
        share = shareRepo.save(share);
        return toShareResponse(share, pet.getName());
    }

    /**
     * Record Sharing: Revoke share request.
     */
    @Transactional
    public RecordShareRequestDto.Response revokeShareRequest(String currentUserId, String requestId) {
        RecordShareRequest share = shareRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Share request not found"));

        Pet pet = petRepo.findById(share.getPetId())
                .orElseThrow(() -> new ResourceNotFoundException("Pet record not found"));

        if (!pet.getOwnerUserId().equals(currentUserId) && !share.getRequesterUserId().equals(currentUserId)) {
            throw new ForbiddenException("Unauthorized to revoke this share grant");
        }

        share.setStatus("REVOKED");
        share.setRevokedAt(Instant.now());
        share = shareRepo.save(share);
        return toShareResponse(share, pet.getName());
    }

    /**
     * Document Vault: Upload document metadata.
     */
    @Transactional
    public PetDocumentDto.Response uploadDocument(String currentUserId, String petId, PetDocumentDto.UploadRequest req) {
        Pet pet = petRepo.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found with ID: " + petId));

        if (!pet.getOwnerUserId().equals(currentUserId)) throw consentDenied();

        PetDocument doc = new PetDocument();
        doc.setDocumentId(idGenerator.newId());
        doc.setPetId(pet.getPetId());
        doc.setUploadedByUserId(currentUserId);
        doc.setCategory(req.category());
        doc.setFileName(req.fileName());
        doc.setFileType(req.fileType());
        doc.setFileSizeBytes(req.fileSizeBytes());
        doc.setStoragePath(req.storagePath());
        doc.setDescription(req.description());
        doc.setDocumentDate(req.documentDate() != null ? req.documentDate() : LocalDate.now());
        doc.setVerificationStatus("OWNER_UPLOADED");

        doc = documentRepo.save(doc);
        return toDocumentResponse(doc, true);
    }

    /**
     * Document Vault: List documents for a pet.
     */
    @Transactional(readOnly = true)
    public List<PetDocumentDto.Response> listDocuments(String currentUserId, String petId, String hospitalId) {
        Pet pet = petRepo.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found with ID: " + petId));

        boolean owner = pet.getOwnerUserId().equals(currentUserId);
        if (!owner && !sharingScopes(currentUserId, pet, hospitalId).contains(SharingScope.FULL_TIMELINE))
            throw consentDenied();

        return documentRepo.findAllByPetIdAndIsDeletedIsFalse(petId).stream()
                .map(doc -> toDocumentResponse(doc, owner))
                .toList();
    }

    // ── Helper Methods ──

    private EnumSet<SharingScope> sharingScopes(String userId, Pet pet, String hospitalId) {
        if (hospitalId == null || hospitalId.isBlank()) throw consentDenied();
        clinicalAccess.requireVeterinarian(userId, hospitalId);

        Instant now = Instant.now();
        var scopes = EnumSet.noneOf(SharingScope.class);
        for (var share : shareRepo.findAllByPetId(pet.getPetId())) {
            if (!hospitalId.equals(share.getTargetHospitalId())
                    || !"APPROVED".equals(share.getStatus())
                    || share.getApprovedAt() == null || share.getApprovedAt().isAfter(now)
                    || share.getRevokedAt() != null
                    || share.getExpiresAt() == null || !share.getExpiresAt().isAfter(now)
                    || !userId.equals(share.getRequesterUserId())) continue;
            for (var scope : SharingScope.values()) {
                if (scope.name().equals(share.getScope())) scopes.add(scope);
            }
        }
        return scopes;
    }

    private ForbiddenException consentDenied() {
        return new ForbiddenException("You do not have active consent for these records at this hospital");
    }

    private PetPassportResponse sharedSummary(Pet pet, PetHealthPassport passport, boolean fullTimeline) {
        return new PetPassportResponse(
                passport.getPassportId(), pet.getPetId(), passport.getPassportNumber(),
                pet.getName(), pet.getSpecies(), pet.getBreed(), pet.getGender(),
                pet.getDob(), calculateAge(pet.getDob()), pet.getWeightKg(), null, null, null,
                passport.getBloodGroup(), passport.getAllergiesSummary(), passport.getChronicConditionsSummary(),
                fullTimeline ? passport.getDietaryNotes() : null, null, null,
                pet.getPhotoUrl(), pet.isDeceased(), null, null, List.of(), passport.getCreatedAt(), passport.getUpdatedAt());
    }

    private String generateQrHash() {
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < 32; i++) {
            sb.append(HEX.charAt(RANDOM.nextInt(HEX.length())));
        }
        return sb.toString();
    }

    private PetPassportResponse toPassportResponse(Pet pet, PetHealthPassport passport) {
        List<PetPassportResponse.PetIdentifierDto> idDtos = identifierRepo.findAllByPetId(pet.getPetId()).stream()
                .map(i -> new PetPassportResponse.PetIdentifierDto(
                        i.getId(),
                        i.getIdentifierType(),
                        i.getIdentifierValue(),
                        i.getIssuingAuthority(),
                        i.getVerifiedAt() != null,
                        i.getVerifiedAt()
                )).toList();

        // Vaccination due
        List<LocalDate> nextVax = vaccinationRepo.findNextVaccinationDateForPet(pet.getPetId(), LocalDate.now(), PageRequest.of(0, 1));
        LocalDate nextDue = nextVax.isEmpty() ? null : nextVax.get(0);

        // Next appointment
        List<Appointment> nextApt = appointmentRepo.findNextAppointmentForPet(pet.getPetId(), pet.getOwnerUserId(), Instant.now(), PageRequest.of(0, 1));
        Instant nextAptInstant = nextApt.isEmpty() ? null : nextApt.get(0).getAppointmentAt();

        return new PetPassportResponse(
                passport.getPassportId(),
                pet.getPetId(),
                passport.getPassportNumber(),
                pet.getName(),
                pet.getSpecies(),
                pet.getBreed(),
                pet.getGender(),
                pet.getDob(),
                calculateAge(pet.getDob()),
                pet.getWeightKg(),
                pet.getColor(),
                pet.getDistinctiveMarks(),
                pet.getMicrochipNumber(),
                passport.getBloodGroup(),
                passport.getAllergiesSummary(),
                passport.getChronicConditionsSummary(),
                passport.getDietaryNotes(),
                passport.getEmergencyContactPhone(),
                passport.getPublicEmergencyQrHash(),
                pet.getPhotoUrl(),
                pet.isDeceased(),
                nextDue,
                nextAptInstant,
                idDtos,
                passport.getCreatedAt(),
                passport.getUpdatedAt()
        );
    }

    private RecordShareRequestDto.Response toShareResponse(RecordShareRequest share, String petName) {
        return new RecordShareRequestDto.Response(
                share.getRequestId(),
                share.getPetId(),
                petName,
                share.getRequesterUserId(),
                share.getTargetHospitalId(),
                share.getScope(),
                share.getStatus(),
                share.getExpiresAt(),
                share.getApprovedAt(),
                share.getRevokedAt(),
                share.getCreatedAt()
        );
    }

    private PetDocumentDto.Response toDocumentResponse(PetDocument doc, boolean owner) {
        return new PetDocumentDto.Response(
                doc.getDocumentId(),
                doc.getPetId(),
                doc.getCategory(),
                doc.getFileName(),
                doc.getFileType(),
                doc.getFileSizeBytes(),
                owner ? doc.getStoragePath() : null,
                doc.getDescription(),
                doc.getDocumentDate(),
                doc.getVerificationStatus(),
                doc.getVerifiedByVetId(),
                doc.getCreatedAt()
        );
    }

    private String calculateAge(LocalDate dob) {
        if (dob == null) return "N/A";
        Period p = Period.between(dob, LocalDate.now());
        if (p.getYears() > 0) {
            return p.getYears() + " yr" + (p.getYears() > 1 ? "s" : "") +
                   (p.getMonths() > 0 ? " " + p.getMonths() + " mo" : "");
        } else if (p.getMonths() > 0) {
            return p.getMonths() + " mo";
        } else {
            return p.getDays() + " days";
        }
    }
}
