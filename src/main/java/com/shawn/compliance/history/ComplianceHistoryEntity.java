package com.shawn.compliance.history;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "compliance_history")
public class ComplianceHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String requestId;
    private String sessionId;
    private String customerId;
    private String decision;

    @Column(length = 2000)
    private String reason;

    @Column(length = 5000)
    private String explanation;

    private String status;
    private Instant createdAt;

    public ComplianceHistoryEntity() {
    }

    public ComplianceHistoryEntity(String requestId,
                                   String sessionId,
                                   String customerId,
                                   String decision,
                                   String reason,
                                   String explanation,
                                   String status) {
        this.requestId = requestId;
        this.sessionId = sessionId;
        this.customerId = customerId;
        this.decision = decision;
        this.reason = reason;
        this.explanation = explanation;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getDecision() {
        return decision;
    }

    public String getReason() {
        return reason;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}