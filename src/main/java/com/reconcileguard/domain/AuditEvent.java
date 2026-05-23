package com.reconcileguard.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "RG_RECON_AUDIT")
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AUDIT_ID")
    private Long id;

    @Column(name = "EVENT_TIME", nullable = false)
    private LocalDateTime eventTime;

    @Column(name = "ACTOR", nullable = false, length = 120)
    private String actor;

    @Column(name = "ACTION", nullable = false, length = 80)
    private String action;

    @Column(name = "REFERENCE_ID", nullable = false, length = 60)
    private String referenceId;

    @Column(name = "DETAILS", length = 800)
    private String details;

    protected AuditEvent() {
    }

    public AuditEvent(String actor, String action, String referenceId, String details) {
        this.eventTime = LocalDateTime.now();
        this.actor = actor;
        this.action = action;
        this.referenceId = referenceId;
        this.details = details;
    }

    public Long getId() { return id; }
    public LocalDateTime getEventTime() { return eventTime; }
    public String getActor() { return actor; }
    public String getAction() { return action; }
    public String getReferenceId() { return referenceId; }
    public String getDetails() { return details; }
}
