package com.pet.vetalya.vet.service;

import com.pet.vetalya.common.GlobalExceptionHandler.*;
import com.pet.vetalya.common.IdGenerator;
import com.pet.vetalya.entity.Hospital;
import com.pet.vetalya.repository.AppointmentRepository;
import com.pet.vetalya.repository.HospitalRepository;
import com.pet.vetalya.vet.dto.HospitalRequest;
import com.pet.vetalya.vet.dto.HospitalResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class HospitalService {

    private static final Logger log = LoggerFactory.getLogger(HospitalService.class);

    private final HospitalRepository hospitalRepo;
    private final AppointmentRepository appointmentRepo;
    private final IdGenerator idGenerator;

    public HospitalService(HospitalRepository hospitalRepo,
                           AppointmentRepository appointmentRepo,
                           IdGenerator idGenerator) {
        this.hospitalRepo = hospitalRepo;
        this.appointmentRepo = appointmentRepo;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public HospitalResponse create(String vetUserId, HospitalRequest request) {
        if (hospitalRepo.countByVetUserIdAndActiveTrue(vetUserId) >= 5) {
            throw new ConflictException("A vet can have at most 5 active hospitals");
        }

        validateTimings(request);

        Hospital h = new Hospital();
        h.setHospitalId(idGenerator.newId());
        h.setVetUserId(vetUserId);
        h.setActive(true);
        applyFields(h, request);

        h = hospitalRepo.save(h);
        log.info("Hospital created: hospitalId={}, vetUserId={}", h.getHospitalId(), vetUserId);
        return toResponse(h);
    }

    @Transactional(readOnly = true)
    public List<HospitalResponse> listOwn(String vetUserId) {
        return hospitalRepo.findAllByVetUserIdAndActiveTrue(vetUserId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public HospitalResponse getOwn(String vetUserId, String hospitalId) {
        Hospital h = findOwnOrThrow(vetUserId, hospitalId);
        return toResponse(h);
    }

    @Transactional
    public HospitalResponse update(String vetUserId, String hospitalId, HospitalRequest request) {
        validateTimings(request);

        Hospital h = findOwnOrThrow(vetUserId, hospitalId);
        applyFields(h, request);

        h = hospitalRepo.save(h);
        log.info("Hospital updated: hospitalId={}, vetUserId={}", hospitalId, vetUserId);
        return toResponse(h);
    }

    @Transactional
    public void delete(String vetUserId, String hospitalId) {
        Hospital h = findOwnOrThrow(vetUserId, hospitalId);

        // Refuse delete if future non-cancelled appointments exist
        if (appointmentRepo.existsFutureNonCancelledByHospitalId(hospitalId, Instant.now())) {
            throw new ConflictException(
                    "Cannot delete hospital with future non-cancelled appointments. Cancel or reassign them first.");
        }

        // Soft delete: is_active = false
        h.setActive(false);
        hospitalRepo.save(h);
        log.info("Hospital soft-deleted: hospitalId={}, vetUserId={}", hospitalId, vetUserId);
    }

    /**
     * Validates timing constraints:
     * - If not 24x7, openTime and closeTime must be provided and openTime must be before closeTime.
     */
    private void validateTimings(HospitalRequest request) {
        if (!request.is24x7()) {
            if (request.openTime() == null || request.closeTime() == null) {
                throw new BadRequestException(
                        "Open time and close time are required when hospital is not 24x7");
            }
            if (!request.openTime().isBefore(request.closeTime())) {
                throw new BadRequestException(
                        "Open time must be before close time");
            }
        }
    }

    private Hospital findOwnOrThrow(String vetUserId, String hospitalId) {
        // Returns 404 for non-owned or inactive hospital (not 403) — as per spec
        return hospitalRepo.findByHospitalIdAndVetUserIdAndActiveTrue(hospitalId, vetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital not found"));
    }

    private void applyFields(Hospital h, HospitalRequest r) {
        h.setName(r.name());
        h.setAddress1(r.address1());
        h.setAddress2(r.address2());
        h.setCity(r.city());
        h.setState(r.state());
        h.setPincode(r.pincode());
        h.setCountry(r.country() != null ? r.country() : "INDIA");
        h.setMobile(r.mobile());
        h.setEmail(r.email());
        h.setGstNumber(r.gstNumber());
        h.setIs24x7(r.is24x7());
        h.setOpenTime(r.is24x7() ? null : r.openTime());
        h.setCloseTime(r.is24x7() ? null : r.closeTime());
    }

    private HospitalResponse toResponse(Hospital h) {
        return new HospitalResponse(
                h.getHospitalId(),
                h.getName(),
                h.getAddress1(),
                h.getAddress2(),
                h.getCity(),
                h.getState(),
                h.getPincode(),
                h.getCountry(),
                h.getMobile(),
                h.getEmail(),
                h.getGstNumber(),
                h.isIs24x7(),
                h.getOpenTime(),
                h.getCloseTime(),
                h.getVersion(),
                h.getCreatedAt(),
                h.getUpdatedAt()
        );
    }
}
