package com.learningos.rag.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "kb_chat_message",
        indexes = @Index(name = "idx_kb_chat_message_lifecycle", columnList = "learner_id,deleted_at,decay_at,created_at")
)
public class KbChatMessage {
    @Id
    private String id;

    @Column(name = "session_id", length = 80)
    private String sessionId;

    @Column(name = "learner_id", length = 120)
    private String learnerId;

    @Column(length = 40)
    private String role = "AI_QA_SUMMARY";

    @Column(name = "content_summary", length = 2000)
    private String contentSummary;

    @Column(name = "source_policy", length = 80)
    private String sourcePolicy;

    @Column(name = "salience_score")
    private Double salienceScore = 0.50;

    @Column(name = "decay_at")
    private Instant decayAt;

    @Column
    private boolean editable = true;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null || id.isBlank()) {
            id = "memm_" + UUID.randomUUID().toString().replace("-", "");
        }
        if (role == null || role.isBlank()) {
            role = "AI_QA_SUMMARY";
        }
        if (salienceScore == null) {
            salienceScore = 0.50;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
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

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getLearnerId() {
        return learnerId;
    }

    public void setLearnerId(String learnerId) {
        this.learnerId = learnerId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContentSummary() {
        return contentSummary;
    }

    public void setContentSummary(String contentSummary) {
        this.contentSummary = contentSummary;
    }

    public String getSourcePolicy() {
        return sourcePolicy;
    }

    public void setSourcePolicy(String sourcePolicy) {
        this.sourcePolicy = sourcePolicy;
    }

    public Double getSalienceScore() {
        return salienceScore;
    }

    public void setSalienceScore(Double salienceScore) {
        this.salienceScore = salienceScore;
    }

    public Instant getDecayAt() {
        return decayAt;
    }

    public void setDecayAt(Instant decayAt) {
        this.decayAt = decayAt;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
