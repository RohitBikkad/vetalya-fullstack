package com.pet.vetalya.vet.service;

import com.pet.vetalya.common.GlobalExceptionHandler.*;
import com.pet.vetalya.common.PageResponse;
import com.pet.vetalya.entity.Appointment;
import com.pet.vetalya.entity.Pet;
import com.pet.vetalya.entity.PetOwner;
import com.pet.vetalya.repository.AppointmentRepository;
import com.pet.vetalya.vet.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("appointmentAt", "createdAt", "status");
    private static final int MAX_PAGE_SIZE = 50;

    // Allowed status transitions
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            "BOOKED", Set.of("CONFIRMED", "CANCELLED"),
            "CONFIRMED", Set.of("COMPLETED", "CANCELLED", "NO_SHOW")
    );

    private final AppointmentRepository appointmentRepo;
    private final com.pet.vetalya.repository.ReviewRepository reviewRepo;
    private final com.pet.vetalya.audit.service.AuditService auditService;
    private final com.pet.vetalya.metrics.AppMetrics appMetrics;

    public AppointmentService(AppointmentRepository appointmentRepo,
                              com.pet.vetalya.repository.ReviewRepository reviewRepo) {
        this(appointmentRepo, reviewRepo, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AppointmentService(AppointmentRepository appointmentRepo,
                              com.pet.vetalya.repository.ReviewRepository reviewRepo,
                              com.pet.vetalya.audit.service.AuditService auditService,
                              com.pet.vetalya.metrics.AppMetrics appMetrics) {
        this.appointmentRepo = appointmentRepo;
        this.reviewRepo = reviewRepo;
        this.auditService = auditService;
        this.appMetrics = appMetrics;
    }

    @Transactional(readOnly = true)
    public PageResponse<AppointmentResponse> listAppointments(
            String vetUserId, String status, String hospitalId,
            LocalDate from, LocalDate to, int page, int size, String sortField, String sortDir) {

        size = Math.min(size, MAX_PAGE_SIZE);
        String field = ALLOWED_SORT_FIELDS.contains(sortField) ? sortField : "appointmentAt";
        Sort sort = "desc".equalsIgnoreCase(sortDir) ? Sort.by(field).descending() : Sort.by(field).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // Convert local dates to IST-day boundaries
        Instant fromInstant = from != null ? from.atStartOfDay(IST).toInstant() : null;
        Instant toInstant = to != null ? to.plusDays(1).atStartOfDay(IST).toInstant() : null;

        Page<Appointment> appointments = appointmentRepo.findVetAppointments(
                vetUserId, status, hospitalId, fromInstant, toInstant, pageable);

        Page<AppointmentResponse> responsePage = appointments.map(a -> toResponse(a));
        return PageResponse.of(responsePage);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getAppointment(String vetUserId, String appointmentId) {
        Appointment a = findOwnOrThrow(vetUserId, appointmentId);
        return toResponse(a);
    }

    @Transactional
    public AppointmentResponse updateStatus(String vetUserId, String appointmentId, String newStatus, String notes) {
        Appointment a = findOwnOrThrow(vetUserId, appointmentId);
        String currentStatus = a.getStatus();

        // Validate transition
        Set<String> allowed = TRANSITIONS.get(currentStatus);
        if (allowed == null || !allowed.contains(newStatus)) {
            throw new ConflictException(
                    "Cannot transition from " + currentStatus + " to " + newStatus);
        }

        // COMPLETED and NO_SHOW only after appointment_at has passed
        if (("COMPLETED".equals(newStatus) || "NO_SHOW".equals(newStatus))
                && a.getAppointmentAt().isAfter(Instant.now())) {
            throw new ConflictException(
                    "Cannot mark as " + newStatus + " before the appointment time has passed");
        }

        a.setStatus(newStatus);
        if (notes != null && !notes.isBlank()) {
            a.setVisitNotes(notes);
        }
        a = appointmentRepo.save(a);

        if (auditService != null) {
            auditService.recordEvent("APPOINTMENT_STATUS_CHANGED", vetUserId, "ROLE_VET", null, "SUCCESS", null,
                    null, null, null, null,
                    java.util.Map.of("appointmentId", appointmentId, "from", currentStatus, "to", newStatus));
        }
        if (appMetrics != null) {
            appMetrics.recordAppointmentStatusChange(currentStatus, newStatus);
        }

        // On cancellation, delete pending unsent reminders
        if ("CANCELLED".equals(newStatus)) {
            appointmentRepo.deletePendingReminders(appointmentId);
            if (appMetrics != null) {
                appMetrics.recordAppointmentCancelled();
            }
            log.info("appointment_cancelled: appointmentId={}", appointmentId);
        } else {
            log.info("appointment_status_changed: appointmentId={}, {} -> {}", appointmentId, currentStatus, newStatus);
        }

        return toResponse(a);
    }

    @Transactional
    public AppointmentResponse updateVisitNotes(String vetUserId, String appointmentId, String visitNotes) {
        Appointment a = findOwnOrThrow(vetUserId, appointmentId);

        // Visit notes can only be set on CONFIRMED or COMPLETED appointments
        if (!"CONFIRMED".equals(a.getStatus()) && !"COMPLETED".equals(a.getStatus())) {
            throw new ConflictException(
                    "Visit notes can only be added to CONFIRMED or COMPLETED appointments");
        }

        a.setVisitNotes(visitNotes);
        a = appointmentRepo.save(a);
        log.info("Visit notes updated: appointmentId={}", appointmentId);
        return toResponse(a);
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(String vetUserId) {
        // Status counts
        List<Object[]> statusCounts = appointmentRepo.countByStatusForVet(vetUserId);
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Object[] row : statusCounts) {
            counts.put((String) row[0], (Long) row[1]);
        }

        // Today's count (IST day)
        LocalDate today = LocalDate.now(IST);
        Instant dayStart = today.atStartOfDay(IST).toInstant();
        Instant dayEnd = today.plusDays(1).atStartOfDay(IST).toInstant();
        long todayCount = appointmentRepo.countTodayForVet(vetUserId, dayStart, dayEnd);

        // Upcoming 7 days (from today's start up to 7 days later)
        Instant upcomingEnd = today.plusDays(8).atStartOfDay(IST).toInstant();
        long upcoming7Days = appointmentRepo.countUpcoming7Days(vetUserId, dayStart, upcomingEnd);

        // Pending (BOOKED) requests
        long pendingRequests = appointmentRepo.countPendingRequests(vetUserId);

        // Ratings & review count
        double avgRating = reviewRepo.averageRatingForVet(vetUserId);
        long reviewCount = reviewRepo.countByVetUserId(vetUserId);

        return new DashboardResponse(
                todayCount,
                upcoming7Days,
                pendingRequests,
                avgRating,
                reviewCount,
                counts
        );
    }

    private Appointment findOwnOrThrow(String vetUserId, String appointmentId) {
        return appointmentRepo.findByAppointmentIdAndVetUserId(appointmentId, vetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
    }

    private AppointmentResponse toResponse(Appointment a) {
        Pet p = a.getPet();
        PetOwner o = a.getOwner();

        AppointmentResponse.PetSummary petSummary = p != null
                ? new AppointmentResponse.PetSummary(p.getPetId(), p.getName(), p.getSpecies(), p.getBreed(), p.getPhotoUrl())
                : null;

        AppointmentResponse.OwnerSummary ownerSummary = o != null
                ? new AppointmentResponse.OwnerSummary(o.getUserId(), o.getFirstName(), o.getLastName())
                : null;

        return new AppointmentResponse(
                a.getAppointmentId(),
                a.getHospitalId(),
                a.getAppointmentAt(),
                a.getStatus(),
                a.getReason(),
                a.getVisitNotes(),
                a.getVersion(),
                petSummary,
                ownerSummary,
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
