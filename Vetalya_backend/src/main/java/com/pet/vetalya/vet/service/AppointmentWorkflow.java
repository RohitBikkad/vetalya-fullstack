package com.pet.vetalya.vet.service;

import com.pet.vetalya.common.GlobalExceptionHandler.ForbiddenException;
import com.pet.vetalya.entity.Appointment;
import com.pet.vetalya.repository.AppointmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
public class AppointmentWorkflow {
    private final AppointmentRepository appointments;
    private final JdbcTemplate db;

    public AppointmentWorkflow(AppointmentRepository appointments, JdbcTemplate db) {
        this.appointments = appointments;
        this.db = db;
    }

    public Appointment requireAssignedVisit(String appointmentId, String vetUserId) {
        return appointments.findByAppointmentIdAndVetUserId(appointmentId, vetUserId)
                .orElseThrow(() -> new ForbiddenException("Appointment is not assigned to this veterinarian"));
    }

    public void checkInAtHospital(String hospitalId, String appointmentId) {
        int changed = db.update("""
            UPDATE appointment SET check_in_time=NOW(),status='CONFIRMED',updated_at=NOW(),version=version+1
            WHERE appointment_id=? AND hospital_id=? AND status IN ('BOOKED','CONFIRMED') AND check_in_time IS NULL
            """, appointmentId, hospitalId);
        if (changed != 1)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Appointment cannot be checked in");
    }

    public void markEncounterStarted(Appointment appointment) {
        appointment.setStatus("IN_CONSULTATION");
        if (appointment.getConsultationStartTime() == null) {
            appointment.setConsultationStartTime(Instant.now());
        }
        appointments.save(appointment);
    }

    public void completeSignedEncounter(String appointmentId, Instant signedAt, String visitNotes) {
        appointments.findById(appointmentId).ifPresent(appointment -> {
            appointment.setStatus("COMPLETED");
            appointment.setConsultationEndTime(signedAt);
            appointment.setVisitNotes(visitNotes);
            appointments.save(appointment);
        });
    }
}
