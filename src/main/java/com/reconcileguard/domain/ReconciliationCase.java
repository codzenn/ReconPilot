package com.reconcileguard.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "RG_RECON_CASE",
        uniqueConstraints = @UniqueConstraint(name = "uq_rg_case", columnNames = {"TXN_ID", "ISSUE_TYPE"})
)
public class ReconciliationCase {
    @Id
    @Column(name = "CASE_ID", length = 40)
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "TXN_ID", nullable = false)
    private PaymentTransaction transaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "ISSUE_TYPE", nullable = false, length = 80)
    private IssueType issueType;

    @Enumerated(EnumType.STRING)
    @Column(name = "SEVERITY", nullable = false, length = 20)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private CaseStatus status = CaseStatus.OPEN;

    @Column(name = "SLA_HOURS", nullable = false)
    private int slaHours;

    @Column(name = "ROOT_CAUSE", nullable = false, length = 500)
    private String rootCause;

    @Column(name = "RECOMMENDED_ACTION", nullable = false, length = 500)
    private String recommendedAction;

    @Column(name = "OWNER_QUEUE", nullable = false, length = 120)
    private String ownerQueue;

    @Column(name = "RESOLUTION_NOTE", length = 500)
    private String resolutionNote;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    protected ReconciliationCase() {
    }

    public ReconciliationCase(String id, PaymentTransaction transaction, IssueType issueType, Severity severity,
                              int slaHours, String rootCause, String recommendedAction, String ownerQueue) {
        this.id = id;
        this.transaction = transaction;
        this.issueType = issueType;
        this.severity = severity;
        this.slaHours = slaHours;
        this.rootCause = rootCause;
        this.recommendedAction = recommendedAction;
        this.ownerQueue = ownerQueue;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void refresh(Severity severity, int slaHours, String rootCause, String recommendedAction, String ownerQueue) {
        this.severity = severity;
        this.slaHours = slaHours;
        this.rootCause = rootCause;
        this.recommendedAction = recommendedAction;
        this.ownerQueue = ownerQueue;
        this.updatedAt = LocalDateTime.now();
    }

    public void resolve(String resolutionNote) {
        this.status = CaseStatus.RESOLVED;
        this.resolutionNote = resolutionNote;
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public PaymentTransaction getTransaction() { return transaction; }
    public IssueType getIssueType() { return issueType; }
    public Severity getSeverity() { return severity; }
    public CaseStatus getStatus() { return status; }
    public int getSlaHours() { return slaHours; }
    public String getRootCause() { return rootCause; }
    public String getRecommendedAction() { return recommendedAction; }
    public String getOwnerQueue() { return ownerQueue; }
    public String getResolutionNote() { return resolutionNote; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
