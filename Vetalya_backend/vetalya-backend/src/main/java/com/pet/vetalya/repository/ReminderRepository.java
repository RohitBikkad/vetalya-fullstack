package com.pet.vetalya.repository;

import com.pet.vetalya.entity.Reminder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;

public interface ReminderRepository extends JpaRepository<Reminder, String> {

    /**
     * Upcoming reminders for a vet: reminders linked to the vet's appointments,
     * where remind_at >= now, sorted by remind_at ascending.
     */
    @Query("""
            SELECT r FROM Reminder r
            JOIN r.appointment a
            WHERE a.vetUserId = :vetUserId
              AND r.remindAt >= :now
              AND r.sent = false
            ORDER BY r.remindAt ASC
            """)
    Page<Reminder> findUpcomingForVet(String vetUserId, Instant now, Pageable pageable);

    @Query("""
            SELECT r FROM Reminder r
            WHERE r.userId = :userId
              AND r.remindAt >= :now
              AND r.sent = false
            ORDER BY r.remindAt ASC
            """)
    Page<Reminder> findUpcomingForUser(String userId, Instant now, Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM Reminder r WHERE r.appointmentId = :appointmentId AND r.sent = false")
    void deleteUnsentByAppointmentId(String appointmentId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM Reminder r WHERE r.userId = :userId AND r.sent = false")
    void deleteAllUnsentByUserId(String userId);

    @Query("SELECT r FROM Reminder r WHERE r.sent = false AND r.remindAt <= :now ORDER BY r.remindAt ASC")
    org.springframework.data.domain.Page<Reminder> findDueReminders(@org.springframework.data.repository.query.Param("now") Instant now, Pageable pageable);

    @Query("SELECT COUNT(r) FROM Reminder r WHERE r.sent = false AND r.remindAt <= :now")
    long countOverdueReminders(@org.springframework.data.repository.query.Param("now") Instant now);
}
