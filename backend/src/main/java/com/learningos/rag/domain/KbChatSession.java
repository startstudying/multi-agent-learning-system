package com.learningos.rag.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "kb_chat_session")
public class KbChatSession {
    @Id
    private String id;

    @Column(name = "learner_id", length = 120)
    private String learnerId;

    @Column(name = "course_id", length = 120)
    private String courseId;

    @Column(length = 255)
    private String title;

    @Column(length = 40)
    private String status = "ACTIVE";

    @Column(name = "salience_score")
    private Double salienceScore = 0.50;

    @Column(name = "decay_at")
    private Instant decayAt;

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
            id = "mems_" + UUID.randomUUID().toString().replace("-", "");
        }
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
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

    public String getLearnerId() {
        return learnerId;
    }

    public void setLearnerId(String learnerId) {
        this.learnerId = learnerId;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
