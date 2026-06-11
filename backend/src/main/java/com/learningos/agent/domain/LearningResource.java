package com.learningos.agent.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "learning_resource")
public class LearningResource {

    @Id
    private String id;

    @Column(nullable = false)
    private String generationTaskId;

    @Column(nullable = false)
    private String learnerId;

    @Column(nullable = false)
    private String resourceType;

    @Column(nullable = false)
    private String modality;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String reviewStatus;

    @Column(length = 2000)
    private String citationSummary;

    @Column(nullable = false, length = 12000)
    private String markdownContent;

    @Column(nullable = false)
    private String safetyStatus;

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

    public String getGenerationTaskId() {
        return generationTaskId;
    }

    public void setGenerationTaskId(String generationTaskId) {
        this.generationTaskId = generationTaskId;
    }

    public String getLearnerId() {
        return learnerId;
    }

    public void setLearnerId(String learnerId) {
        this.learnerId = learnerId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getModality() {
        return modality;
    }

    public void setModality(String modality) {
        this.modality = modality;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public String getCitationSummary() {
        return citationSummary;
    }

    public void setCitationSummary(String citationSummary) {
        this.citationSummary = citationSummary;
    }

    public String getMarkdownContent() {
        return markdownContent;
    }

    public void setMarkdownContent(String markdownContent) {
        this.markdownContent = markdownContent;
    }

    public String getSafetyStatus() {
        return safetyStatus;
    }

    public void setSafetyStatus(String safetyStatus) {
        this.safetyStatus = safetyStatus;
    }
}
