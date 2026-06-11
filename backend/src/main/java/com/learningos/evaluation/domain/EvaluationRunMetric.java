package com.learningos.evaluation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evaluation_run_metric")
public class EvaluationRunMetric {

    @Id
    @Column(nullable = false, length = 80)
    private String id;

    @Column(nullable = false, length = 80)
    private String runId;

    @Column(nullable = false, length = 120)
    private String metricName;

    @Column(nullable = false)
    private double metricValue;

    @Column(nullable = false, length = 40)
    private String metricUnit;

    @Column(nullable = false)
    private int sampleCount;

    @Column(columnDefinition = "text")
    private String metricJson;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) {
            id = "evrm_" + UUID.randomUUID().toString().replace("-", "");
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public String getId() {
        return id;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public double getMetricValue() {
        return metricValue;
    }

    public void setMetricValue(double metricValue) {
        this.metricValue = metricValue;
    }

    public String getMetricUnit() {
        return metricUnit;
    }

    public void setMetricUnit(String metricUnit) {
        this.metricUnit = metricUnit;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(int sampleCount) {
        this.sampleCount = sampleCount;
    }

    public String getMetricJson() {
        return metricJson;
    }

    public void setMetricJson(String metricJson) {
        this.metricJson = metricJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
