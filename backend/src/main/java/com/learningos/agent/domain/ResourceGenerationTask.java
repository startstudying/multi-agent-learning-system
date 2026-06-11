package com.learningos.agent.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "resource_generation_task",
        indexes = {
                @Index(name = "idx_rgt_learner", columnList = "learner_id"),
                @Index(name = "idx_rgt_agent_task", columnList = "agent_task_id")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_rgt_learner_request", columnNames = {"learner_id", "request_id"})
)
public class ResourceGenerationTask {

    @Id
    private String id;

    @Column(nullable = false)
    private String learnerId;

    @Column(nullable = false)
    private String goalId;

    @Column(nullable = false)
    private String pathNodeId;

    @Column(length = 120)
    private String requestId;

    @Column(nullable = false)
    private String agentTaskId;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String reviewStatus;

    @Column(nullable = false)
    private Integer progressPercent;

    @Column(nullable = false)
    private Integer retryCount = 0;

    private Instant nextRetryAt;

    @Column(length = 120)
    private String lastError;

    @Column(nullable = false)
    private Boolean recoverable = false;

    @Column(nullable = false)
    private String safetyStatus;

    private String traceId;

    @Column(length = 12000)
    private String profileSnapshot;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
        if (recoverable == null) {
            recoverable = false;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLearnerId() {
        return learnerId;
    }

    public void setLearnerId(String learnerId) {
        this.learnerId = learnerId;
    }

    public String getGoalId() {
        return goalId;
    }

    public void setGoalId(String goalId) {
        this.goalId = goalId;
    }

    public String getPathNodeId() {
        return pathNodeId;
    }

    public void setPathNodeId(String pathNodeId) {
        this.pathNodeId = pathNodeId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getAgentTaskId() {
        return agentTaskId;
    }

    public void setAgentTaskId(String agentTaskId) {
        this.agentTaskId = agentTaskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public Integer getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Integer progressPercent) {
        this.progressPercent = progressPercent;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(Instant nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Boolean getRecoverable() {
        return recoverable;
    }

    public void setRecoverable(Boolean recoverable) {
        this.recoverable = recoverable;
    }

    public String getSafetyStatus() {
        return safetyStatus;
    }

    public void setSafetyStatus(String safetyStatus) {
        this.safetyStatus = safetyStatus;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getProfileSnapshot() {
        return profileSnapshot;
    }

    public void setProfileSnapshot(String profileSnapshot) {
        this.profileSnapshot = profileSnapshot;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
