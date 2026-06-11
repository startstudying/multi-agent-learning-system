package com.learningos.assessment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wrong_question")
public class WrongQuestion {

    @Id
    private String id;

    @Column(nullable = false)
    private String learnerId;

    @Column(nullable = false)
    private String questionId;

    @Column(nullable = false)
    private String answerId;

    @Column(nullable = false)
    private String gradingResultId;

    @Column(nullable = false)
    private String knowledgePointId;

    @Column(nullable = false)
    private Double score;

    @Column(length = 2000)
    private String causeAnalysis;

    @Column(length = 1000)
    private String resourcePushStrategy;

    private String replanRecordId;

    private String traceId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null || id.isBlank()) {
            id = "wq_" + UUID.randomUUID().toString().replace("-", "");
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

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getAnswerId() {
        return answerId;
    }

    public void setAnswerId(String answerId) {
        this.answerId = answerId;
    }

    public String getGradingResultId() {
        return gradingResultId;
    }

    public void setGradingResultId(String gradingResultId) {
        this.gradingResultId = gradingResultId;
    }

    public String getKnowledgePointId() {
        return knowledgePointId;
    }

    public void setKnowledgePointId(String knowledgePointId) {
        this.knowledgePointId = knowledgePointId;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getCauseAnalysis() {
        return causeAnalysis;
    }

    public void setCauseAnalysis(String causeAnalysis) {
        this.causeAnalysis = causeAnalysis;
    }

    public String getResourcePushStrategy() {
        return resourcePushStrategy;
    }

    public void setResourcePushStrategy(String resourcePushStrategy) {
        this.resourcePushStrategy = resourcePushStrategy;
    }

    public String getReplanRecordId() {
        return replanRecordId;
    }

    public void setReplanRecordId(String replanRecordId) {
        this.replanRecordId = replanRecordId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
