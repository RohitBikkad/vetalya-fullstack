package com.pet.vetalya.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.vetalya.common.GlobalExceptionHandler.ConflictException;
import com.pet.vetalya.common.GlobalExceptionHandler.ForbiddenException;
import com.pet.vetalya.common.GlobalExceptionHandler.ResourceNotFoundException;
import com.pet.vetalya.common.IdGenerator;
import com.pet.vetalya.dto.*;
import com.pet.vetalya.entity.*;
import com.pet.vetalya.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

@Service
public class ClinicalEncounterService {

    private static final Logger log = LoggerFactory.getLogger(ClinicalEncounterService.class);
    private static final String HEX = "0123456789abcdef";

    private final ClinicalEncounterRepository encounterRepo;
    private final EncounterAmendmentRepository amendmentRepo;
    private final PrescriptionRepository prescriptionRepo;
    private final PetRepository petRepo;
    private final AppointmentRepository appointmentRepo;
    private final VetRepository vetRepo;
    private final HospitalRepository hospitalRepo;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public ClinicalEncounterService(ClinicalEncounterRepository encounterRepo,
                                    EncounterAmendmentRepository amendmentRepo,
                                    PrescriptionRepository prescriptionRepo,
                                    PetRepository petRepo,
                                    AppointmentRepository appointmentRepo,
                                    VetRepository vetRepo,
                                    HospitalRepository hospitalRepo,
                                    IdGenerator idGenerator,
                                    ObjectMapper objectMapper) {
        this.encounterRepo = encounterRepo;
        this.amendmentRepo = amendmentRepo;
        this.prescriptionRepo = prescriptionRepo;
        this.petRepo = petRepo;
        this.appointmentRepo = appointmentRepo;
        this.vetRepo = vetRepo;
        this.hospitalRepo = hospitalRepo;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    /**
     * Create or draft a new clinical encounter.
     */
    @Transactional
    public EncounterResponse createEncounter(String vetUserId, EncounterRequest req) {
        Pet pet = petRepo.findById(req.petId())
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found with ID: " + req.petId()));

        ClinicalEncounter encounter = new ClinicalEncounter();
        encounter.setEncounterId(idGenerator.newId());
        encounter.setPetId(pet.getPetId());
        encounter.setVetUserId(vetUserId);
        encounter.setHospitalId(req.hospitalId());
        encounter.setAppointmentId(req.appointmentId());
        encounter.setEncounterType(req.encounterType() != null ? req.encounterType() : "OPD_CONSULTATION");

        populateEncounterFields(encounter, req);
        encounter = encounterRepo.save(encounter);

        // Handle medications / prescription
        if (req.medications() != null && !req.medications().isEmpty()) {
            savePrescription(encounter, vetUserId, req.medications(), req.dietInstructions());
        }

        // Link with appointment if provided
        if (req.appointmentId() != null && !req.appointmentId().isBlank()) {
            appointmentRepo.findById(req.appointmentId()).ifPresent(apt -> {
                apt.setStatus("IN_CONSULTATION");
                appointmentRepo.save(apt);
            });
        }

        log.info("Clinical encounter created: encounterId={}, vetUserId={}, petId={}",
                encounter.getEncounterId(), vetUserId, pet.getPetId());
        return toEncounterResponse(encounter);
    }

    /**
     * Update an existing draft encounter. Fails if already signed.
     */
    @Transactional
    public EncounterResponse updateDraftEncounter(String vetUserId, String encounterId, EncounterRequest req) {
        ClinicalEncounter encounter = encounterRepo.findById(encounterId)
                .orElseThrow(() -> new ResourceNotFoundException("Encounter not found with ID: " + encounterId));

        if (!encounter.getVetUserId().equals(vetUserId)) {
            throw new ForbiddenException("Only the authoring veterinarian can update this encounter draft");
        }

        if (encounter.isSigned()) {
            throw new ConflictException("Signed clinical records are immutable. Use the amendment workflow to document corrections.");
        }

        populateEncounterFields(encounter, req);
        encounter = encounterRepo.save(encounter);

        // Update medications
        if (req.medications() != null) {
            prescriptionRepo.findByEncounterId(encounterId).ifPresent(prescriptionRepo::delete);
            if (!req.medications().isEmpty()) {
                savePrescription(encounter, vetUserId, req.medications(), req.dietInstructions());
            }
        }

        return toEncounterResponse(encounter);
    }

    /**
     * Cryptographically sign and finalize the clinical encounter.
     * Generates a tamper-evident SHA-256 signature hash and marks record immutable.
     */
    @Transactional
    public EncounterResponse signEncounter(String vetUserId, String encounterId) {
        ClinicalEncounter encounter = encounterRepo.findById(encounterId)
                .orElseThrow(() -> new ResourceNotFoundException("Encounter not found with ID: " + encounterId));

        if (!encounter.getVetUserId().equals(vetUserId)) {
            throw new ForbiddenException("Only the attending veterinarian can sign and finalize this encounter");
        }

        if (encounter.isSigned()) {
            return toEncounterResponse(encounter);
        }

        Instant signTimestamp = Instant.now();
        String signatureHash = computeSignatureHash(encounter, signTimestamp);

        encounter.setSigned(true);
        encounter.setSignedAt(signTimestamp);
        encounter.setDigitalSignatureHash(signatureHash);
        final ClinicalEncounter savedEncounter = encounterRepo.save(encounter);

        // Complete linked appointment
        if (savedEncounter.getAppointmentId() != null) {
            appointmentRepo.findById(savedEncounter.getAppointmentId()).ifPresent(apt -> {
                apt.setStatus("COMPLETED");
                // Store serialized summary in visit_notes for legacy compatibility
                try {
                    Map<String, Object> summary = new HashMap<>();
                    summary.put("assessment", savedEncounter.getDiagnosisPrimary());
                    summary.put("treatmentPlan", savedEncounter.getTreatmentPlan());
                    summary.put("temperature", savedEncounter.getTemperatureF());
                    summary.put("weight", savedEncounter.getBodyWeightKg());
                    summary.put("dietPlan", savedEncounter.getDietInstructions());
                    summary.put("signedAt", signTimestamp.toString());
                    summary.put("digitalSignatureHash", signatureHash);
                    apt.setVisitNotes(objectMapper.writeValueAsString(summary));
                } catch (Exception e) {
                    apt.setVisitNotes(savedEncounter.getDiagnosisPrimary());
                }
                appointmentRepo.save(apt);
            });
        }

        log.info("Clinical encounter digitally signed: encounterId={}, hash={}", encounterId, signatureHash);
        return toEncounterResponse(savedEncounter);
    }

    /**
     * Create an auditable amendment to a signed clinical encounter.
     * Captures a before-and-after snapshot and logs justification.
     */
    @Transactional
    public EncounterResponse amendEncounter(String vetUserId, String encounterId, EncounterAmendmentRequest req) {
        ClinicalEncounter encounter = encounterRepo.findById(encounterId)
                .orElseThrow(() -> new ResourceNotFoundException("Encounter not found with ID: " + encounterId));

        if (!encounter.isSigned()) {
            throw new ConflictException("Encounter is not yet signed. Update draft directly instead of creating an amendment.");
        }

        // Snapshot prior state
        String priorSnapshot;
        try {
            priorSnapshot = objectMapper.writeValueAsString(toEncounterResponse(encounter));
        } catch (Exception e) {
            priorSnapshot = "{\"error\": \"Serialization failed\"}";
        }

        // Apply changes
        if (req.chiefComplaint() != null) encounter.setChiefComplaint(req.chiefComplaint());
        if (req.physicalExamination() != null) encounter.setPhysicalExamination(req.physicalExamination());
        if (req.temperatureF() != null) encounter.setTemperatureF(req.temperatureF());
        if (req.heartRateBpm() != null) encounter.setHeartRateBpm(req.heartRateBpm());
        if (req.respiratoryRateBpm() != null) encounter.setRespiratoryRateBpm(req.respiratoryRateBpm());
        if (req.bodyWeightKg() != null) encounter.setBodyWeightKg(req.bodyWeightKg());
        if (req.assessmentNotes() != null) encounter.setAssessmentNotes(req.assessmentNotes());
        if (req.diagnosisPrimary() != null) encounter.setDiagnosisPrimary(req.diagnosisPrimary());
        if (req.differentialDiagnosis() != null) encounter.setDifferentialDiagnosis(req.differentialDiagnosis());
        if (req.treatmentPlan() != null) encounter.setTreatmentPlan(req.treatmentPlan());
        if (req.dietInstructions() != null) encounter.setDietInstructions(req.dietInstructions());
        if (req.followUpDate() != null) encounter.setFollowUpDate(req.followUpDate());

        encounter = encounterRepo.save(encounter);

        // Snapshot amended state
        String amendedSnapshot;
        try {
            amendedSnapshot = objectMapper.writeValueAsString(toEncounterResponse(encounter));
        } catch (Exception e) {
            amendedSnapshot = "{}";
        }

        EncounterAmendment amendment = new EncounterAmendment();
        amendment.setAmendmentId(idGenerator.newId());
        amendment.setEncounterId(encounter.getEncounterId());
        amendment.setAmendedByVetId(vetUserId);
        amendment.setAmendmentReason(req.amendmentReason());
        amendment.setPriorSnapshotJson(priorSnapshot);
        amendedContentJson(amendment, amendedSnapshot);
        amendmentRepo.save(amendment);

        log.info("Clinical encounter amended: encounterId={}, amendmentId={}, reason={}",
                encounterId, amendment.getAmendmentId(), req.amendmentReason());

        return toEncounterResponse(encounter);
    }

    private void amendedContentJson(EncounterAmendment amendment, String json) {
        amendment.setAmendedContentJson(json);
    }

    /**
     * Get a single encounter by ID with prescriptions and amendment history.
     */
    @Transactional(readOnly = true)
    public EncounterResponse getEncounter(String encounterId) {
        ClinicalEncounter encounter = encounterRepo.findById(encounterId)
                .orElseThrow(() -> new ResourceNotFoundException("Encounter not found with ID: " + encounterId));
        return toEncounterResponse(encounter);
    }

    /**
     * List all signed clinical encounters for a pet.
     */
    @Transactional(readOnly = true)
    public List<EncounterResponse> listPetEncounters(String petId) {
        return encounterRepo.findAllByPetIdOrderByCreatedAtDesc(petId).stream()
                .map(this::toEncounterResponse)
                .toList();
    }

    // ── Helper Methods ──

    private void populateEncounterFields(ClinicalEncounter encounter, EncounterRequest req) {
        encounter.setChiefComplaint(req.chiefComplaint());
        encounter.setPresentingHistory(req.presentingHistory());
        encounter.setPhysicalExamination(req.physicalExamination());
        encounter.setTemperatureF(req.temperatureF());
        encounter.setHeartRateBpm(req.heartRateBpm());
        encounter.setRespiratoryRateBpm(req.respiratoryRateBpm());
        encounter.setBodyWeightKg(req.bodyWeightKg());
        encounter.setBodyConditionScore(req.bodyConditionScore());
        encounter.setAssessmentNotes(req.assessmentNotes());
        encounter.setDiagnosisPrimary(req.diagnosisPrimary());
        encounter.setDifferentialDiagnosis(req.differentialDiagnosis());
        encounter.setTreatmentPlan(req.treatmentPlan());
        encounter.setDietInstructions(req.dietInstructions());
        encounter.setFollowUpDate(req.followUpDate());
    }

    private void savePrescription(ClinicalEncounter encounter, String vetUserId,
                                  List<EncounterRequest.PrescriptionItemRequest> medRequests,
                                  String instructions) {
        Prescription rx = new Prescription();
        rx.setPrescriptionId(idGenerator.newId());
        rx.setEncounterId(encounter.getEncounterId());
        rx.setPetId(encounter.getPetId());
        rx.setVetUserId(vetUserId);
        rx.setInstructions(instructions);

        for (EncounterRequest.PrescriptionItemRequest m : medRequests) {
            if (m.medicineName() == null || m.medicineName().isBlank()) continue;
            PrescriptionItem item = new PrescriptionItem();
            item.setPrescription(rx);
            item.setPrescriptionId(rx.getPrescriptionId());
            item.setMedicineName(m.medicineName().trim());
            item.setGenericComposition(m.genericComposition());
            item.setDosage(m.dosage() != null ? m.dosage() : "1 unit");
            item.setFrequency(m.frequency() != null ? m.frequency() : "BID");
            item.setDuration(m.duration() != null ? m.duration() : "5 days");
            item.setInstructions(m.instructions());
            item.setScheduleH(m.isScheduleH());
            item.setQuantity(m.quantity() != null ? m.quantity() : 1);
            rx.getItems().add(item);
        }

        prescriptionRepo.save(rx);
    }

    private String computeSignatureHash(ClinicalEncounter encounter, Instant timestamp) {
        try {
            String data = encounter.getEncounterId() + "|" +
                    encounter.getVetUserId() + "|" +
                    encounter.getPetId() + "|" +
                    encounter.getDiagnosisPrimary() + "|" +
                    (encounter.getTreatmentPlan() != null ? encounter.getTreatmentPlan() : "") + "|" +
                    timestamp.toEpochMilli();

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }
    }

    private EncounterResponse toEncounterResponse(ClinicalEncounter e) {
        String petName = petRepo.findById(e.getPetId()).map(Pet::getName).orElse("Patient");
        String vetName = vetRepo.findById(e.getVetUserId())
                .map(v -> "Dr. " + (v.getFirstName() != null ? v.getFirstName() : "") + " " + (v.getLastName() != null ? v.getLastName() : ""))
                .orElse("Attending Veterinarian");
        String hospitalName = e.getHospitalId() != null
                ? hospitalRepo.findById(e.getHospitalId()).map(Hospital::getName).orElse("Vetalya Clinic")
                : "Vetalya Clinic";

        // Prescription mapping
        EncounterResponse.PrescriptionResponse rxDto = prescriptionRepo.findByEncounterId(e.getEncounterId())
                .map(rx -> new EncounterResponse.PrescriptionResponse(
                        rx.getPrescriptionId(),
                        rx.getRxDate(),
                        rx.getInstructions(),
                        rx.getPdfStorageUrl(),
                        rx.isDispensed(),
                        rx.getItems().stream().map(i -> new EncounterResponse.PrescriptionItemDto(
                                i.getItemId(),
                                i.getMedicineName(),
                                i.getGenericComposition(),
                                i.getDosage(),
                                i.getFrequency(),
                                i.getDuration(),
                                i.getInstructions(),
                                i.isScheduleH(),
                                i.getQuantity()
                        )).toList()
                )).orElse(null);

        // Amendments mapping
        List<EncounterResponse.AmendmentResponse> amendmentDtos = amendmentRepo.findAllByEncounterIdOrderByCreatedAtDesc(e.getEncounterId())
                .stream().map(a -> new EncounterResponse.AmendmentResponse(
                        a.getAmendmentId(),
                        a.getAmendedByVetId(),
                        a.getAmendmentReason(),
                        a.getCreatedAt()
                )).toList();

        return new EncounterResponse(
                e.getEncounterId(),
                e.getPetId(),
                petName,
                e.getVetUserId(),
                vetName,
                e.getHospitalId(),
                hospitalName,
                e.getAppointmentId(),
                e.getEncounterType(),
                e.getChiefComplaint(),
                e.getPresentingHistory(),
                e.getPhysicalExamination(),
                e.getTemperatureF(),
                e.getHeartRateBpm(),
                e.getRespiratoryRateBpm(),
                e.getBodyWeightKg(),
                e.getBodyConditionScore(),
                e.getAssessmentNotes(),
                e.getDiagnosisPrimary(),
                e.getDifferentialDiagnosis(),
                e.getTreatmentPlan(),
                e.getDietInstructions(),
                e.getFollowUpDate(),
                e.isSigned(),
                e.getSignedAt(),
                e.getDigitalSignatureHash(),
                e.isPassportPublished(),
                rxDto,
                amendmentDtos,
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}

