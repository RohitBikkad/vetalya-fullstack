package com.pet.vetalya.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "encounter_amendment")
public class EncounterAmendment {

    @Id
    @Column(name = "amendment_id", length = 26)
    private String amendmentId;

    @Column(name = "encounter_id", length = 26, nullable = false)
    private String encounterId;

    @Column(name = "amended_by_vet_id", length = 26, nullable = false)
    private String amendedByVetId;

    @Column(name = "amendment_reason", nullable = false, columnDefinition = "TEXT")
    private String amendmentReason;

    @Column(name = "prior_snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String priorSnapshotJson;

    @Column(name = "amended_content_json", nullable = false, columnDefinition = "TEXT")
    private String amendedContentJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encounter_id", insertable = false, updatable = false)
    private ClinicalEncounter encounter;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    // ── Getters / Setters ──

    public String getAmendmentId() { return amendmentId; }
    public void setAmendmentId(String amendmentId) { this.amendmentId = amendmentId; }

    public String getEncounterId() { return encounterId; }
    public void setEncounterId(String encounterId) { this.encounterId = encounterId; }

    public String getAmendedByVetId() { return amendedByVetId; }
    public void setAmendedByVetId(String amendedByVetId) { this.amendedByVetId = amendedByVetId; }

    public String getAmendmentReason() { return amendmentReason; }
    public void setAmendmentReason(String amendmentReason) { this.amendmentReason = amendmentReason; }

    public String getPriorSnapshotJson() { return priorSnapshotJson; }
    public void setPriorSnapshotJson(String priorSnapshotJson) { this.priorSnapshotJson = priorSnapshotJson; }

    public String getAmendedContentJson() { return amendedContentJson; }
    public void setAmendedContentJson(String amendedContentJson) { this.amendedContentJson = amendedContentJson; }

    public Instant getCreatedAt() { return createdAt; }

    public ClinicalEncounter getEncounter() { return encounter; }
    public void setEncounter(ClinicalEncounter encounter) { this.encounter = encounter; }
}

