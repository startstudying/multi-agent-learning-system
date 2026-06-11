package com.learningos.agent.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resource_review")
public class ResourceReview {

    @Id
    private String id;

    @Column(nullable = false)
    private String generationTaskId;

    @Column(nullable = false)
    private String resourceId;

    @Column(nullable = false)
    private String reviewerType;

    @Column(nullable = false)
    private String status;

    @Column(length = 2000)
    private String summary;

    @Column(length = 2000)
    private String reason;

    @Column(length = 2000)
    private String citationCheck;

    @Column(length = 2000)
    private String safetyCheck;

    @Column(length = 2000)
    private String revisionSuggestion;

    private String traceId;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) {
            id = "rrv_" + UUID.randomUUID().toString().replace("-", "");
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGenerationTaskId() {
        return generationTaskId;
    }

    public void setGenerationTaskId(String generationTaskId) {
        this.generationTaskId = generationTaskId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getReviewerType() {
        return reviewerType;
    }

    public void setReviewerType(String reviewerType) {
        this.reviewerType = reviewerType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getCitationCheck() {
        return citationCheck;
    }

    public void setCitationCheck(String citationCheck) {
        this.citationCheck = citationCheck;
    }

    public String getSafetyCheck() {
        return safetyCheck;
    }

    public void setSafetyCheck(String safetyCheck) {
        this.safetyCheck = safetyCheck;
    }

    public String getRevisionSuggestion() {
        return revisionSuggestion;
    }

    public void setRevisionSuggestion(String revisionSuggestion) {
        this.revisionSuggestion = revisionSuggestion;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
