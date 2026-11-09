package com.pet.vetalya.repository;

import com.pet.vetalya.entity.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, String> {

    Optional<Appointment> findByAppointmentIdAndVetUserId(String appointmentId, String vetUserId);

    /**
     * Paginated list with optional filters. Uses JPQL with dynamic conditions.
     */
    @Query("""
            SELECT a FROM Appointment a
            JOIN FETCH a.pet p
            JOIN FETCH a.owner o
            WHERE a.vetUserId = :vetUserId
              AND (:status IS NULL OR a.status = :status)
              AND (:hospitalId IS NULL OR a.hospitalId = :hospitalId)
              AND (CAST(:from AS timestamp) IS NULL OR a.appointmentAt >= :from)
              AND (CAST(:to AS timestamp) IS NULL OR a.appointmentAt <= :to)
            """)
    Page<Appointment> findVetAppointments(
            String vetUserId,
            String status,
            String hospitalId,
            Instant from,
            Instant to,
            Pageable pageable);

    /**
     * Dashboard: count appointments per status for a vet.
     */
    @Query("""
            SELECT a.status, COUNT(a) FROM Appointment a
            WHERE a.vetUserId = :vetUserId
            GROUP BY a.status
            """)
    List<Object[]> countByStatusForVet(String vetUserId);

    /**
     * Dashboard: today's appointment count for a vet.
     */
    @Query("""
            SELECT COUNT(a) FROM Appointment a
            WHERE a.vetUserId = :vetUserId
              AND a.appointmentAt >= :dayStart
              AND a.appointmentAt < :dayEnd
            """)
    long countTodayForVet(String vetUserId, Instant dayStart, Instant dayEnd);

    /**
     * Dashboard: count upcoming appointments for next 7 days.
     */
    @Query("""
            SELECT COUNT(a) FROM Appointment a
            WHERE a.vetUserId = :vetUserId
              AND a.status IN ('BOOKED', 'CONFIRMED')
              AND a.appointmentAt >= :start
              AND a.appointmentAt < :end
            """)
    long countUpcoming7Days(String vetUserId, Instant start, Instant end);

    /**
     * Dashboard: count pending (BOOKED) requests for a vet.
     */
    @Query("""
            SELECT COUNT(a) FROM Appointment a
            WHERE a.vetUserId = :vetUserId
              AND a.status = 'BOOKED'
            """)
    long countPendingRequests(String vetUserId);

    /**
     * Check if any future non-cancelled appointments exist for a hospital.
     */
    @Query("""
            SELECT COUNT(a) > 0 FROM Appointment a
            WHERE a.hospitalId = :hospitalId
              AND a.status NOT IN ('CANCELLED')
              AND a.appointmentAt > :now
            """)
    boolean existsFutureNonCancelledByHospitalId(String hospitalId, Instant now);

    /**
     * Check if vet has at least one non-cancelled appointment for a pet.
     */
    @Query("""
            SELECT COUNT(a) > 0 FROM Appointment a
            WHERE a.vetUserId = :vetUserId
              AND a.petId = :petId
              AND a.status NOT IN ('CANCELLED')
            """)
    boolean existsNonCancelledForVetAndPet(String vetUserId, String petId);

    /**
     * Find future BOOKED/CONFIRMED appointments for a vet (used during account deletion).
     */
    @Query("""
            SELECT a FROM Appointment a
            WHERE a.vetUserId = :vetUserId
              AND a.status IN ('BOOKED', 'CONFIRMED')
              AND a.appointmentAt > :now
            """)
    List<Appointment> findFutureBookedOrConfirmed(String vetUserId, Instant now);

    /**
     * Delete pending (unsent) reminders for an appointment.
     */
    @Modifying
    @Query("""
            DELETE FROM com.pet.vetalya.entity.Reminder r
            WHERE r.appointmentId = :appointmentId
              AND r.sent = false
            """)
    void deletePendingReminders(String appointmentId);

    // ── Owner queries ──

    Optional<Appointment> findByAppointmentIdAndOwnerUserId(String appointmentId, String ownerUserId);

    @Query("""
            SELECT a FROM Appointment a
            JOIN FETCH a.pet p
            LEFT JOIN FETCH a.hospital h
            WHERE a.ownerUserId = :ownerUserId
              AND (:status IS NULL OR a.status = :status)
              AND (:petId IS NULL OR a.petId = :petId)
              AND (CAST(:from AS timestamp) IS NULL OR a.appointmentAt >= :from)
              AND (CAST(:to AS timestamp) IS NULL OR a.appointmentAt <= :to)
            """)
    Page<Appointment> findOwnerAppointments(
            String ownerUserId,
            String status,
            String petId,
            Instant from,
            Instant to,
            Pageable pageable);

    @Query("""
            SELECT COUNT(a) > 0 FROM Appointment a
            WHERE a.vetUserId = :vetUserId
              AND a.appointmentAt = :appointmentAt
              AND a.status <> 'CANCELLED'
            """)
    boolean existsSlotForVet(String vetUserId, Instant appointmentAt);

    @Query("""
            SELECT COUNT(a) > 0 FROM Appointment a
            WHERE a.petId = :petId
              AND a.appointmentAt = :appointmentAt
              AND a.status <> 'CANCELLED'
            """)
    boolean existsSlotForPet(String petId, Instant appointmentAt);

    @Query("""
            SELECT COUNT(a) > 0 FROM Appointment a
            WHERE a.petId = :petId
              AND a.status IN ('BOOKED', 'CONFIRMED')
              AND a.appointmentAt > :now
            """)
    boolean existsFutureBookedOrConfirmedForPet(String petId, Instant now);

    @Query("""
            SELECT a FROM Appointment a
            WHERE a.ownerUserId = :ownerUserId
              AND a.status IN ('BOOKED', 'CONFIRMED')
              AND a.appointmentAt > :now
            """)
    List<Appointment> findFutureBookedOrConfirmedForOwner(String ownerUserId, Instant now);

    @Query("""
            SELECT a FROM Appointment a
            WHERE a.petId = :petId
              AND a.ownerUserId = :ownerUserId
              AND a.status IN ('BOOKED', 'CONFIRMED')
              AND a.appointmentAt >= :now
            ORDER BY a.appointmentAt ASC
            """)
    List<Appointment> findNextAppointmentForPet(String petId, String ownerUserId, Instant now, Pageable pageable);

    @Query("""
            SELECT a.appointmentAt FROM Appointment a
            WHERE a.vetUserId = :vetUserId
              AND a.status <> 'CANCELLED'
              AND a.appointmentAt >= :start
              AND a.appointmentAt < :end
            """)
    List<Instant> findBookedSlotsForVet(String vetUserId, Instant start, Instant end);

    @Query("""
            SELECT a FROM Appointment a
            JOIN FETCH a.pet p
            LEFT JOIN FETCH a.hospital h
            WHERE a.ownerUserId = :ownerUserId
              AND a.status IN ('BOOKED', 'CONFIRMED')
              AND a.appointmentAt >= :now
            ORDER BY a.appointmentAt ASC
            """)
    List<Appointment> findNext3UpcomingForOwner(String ownerUserId, Instant now, Pageable pageable);

    @Query("""
            SELECT COUNT(a) FROM Appointment a
            WHERE a.ownerUserId = :ownerUserId
              AND a.status = 'BOOKED'
            """)
    long countPendingForOwner(String ownerUserId);

    @Query("""
            SELECT COUNT(a) FROM Appointment a
            WHERE a.ownerUserId = :ownerUserId
              AND a.status = 'COMPLETED'
              AND NOT EXISTS (
                  SELECT r FROM Review r WHERE r.appointmentId = a.appointmentId AND r.ownerUserId = :ownerUserId
              )
            """)
    long countUnreviewedCompletedAppointments(String ownerUserId);
}
