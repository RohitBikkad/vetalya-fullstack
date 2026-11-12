package com.pet.vetalya.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "reminder")
public class Reminder {

    @Id
    @Column(name = "reminder_id", length = 26)
    private String reminderId;

    @Column(name = "appointment_id", length = 26, nullable = false)
    private String appointmentId;

    @Column(name = "user_id", length = 26)
    private String userId;

    @Column(name = "pet_id", length = 26)
    private String petId;

    @Column(name = "remind_at", nullable = false)
    private Instant remindAt;

    @Column(length = 500)
    private String message;

    @Column(name = "is_sent", nullable = false)
    private boolean sent;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", insertable = false, updatable = false)
    private Appointment appointment;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    // ── Getters / Setters ──

    public String getReminderId() { return reminderId; }
    public void setReminderId(String reminderId) { this.reminderId = reminderId; }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPetId() { return petId; }
    public void setPetId(String petId) { this.petId = petId; }

    public Instant getRemindAt() { return remindAt; }
    public void setRemindAt(Instant remindAt) { this.remindAt = remindAt; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isSent() { return sent; }
    public void setSent(boolean sent) { this.sent = sent; }

    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }

    public Instant getCreatedAt() { return createdAt; }

    public Appointment getAppointment() { return appointment; }
}
