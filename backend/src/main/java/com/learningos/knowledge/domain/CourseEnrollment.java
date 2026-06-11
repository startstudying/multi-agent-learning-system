package com.learningos.knowledge.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "course_enrollment",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_course_enrollment_course_learner",
                columnNames = {"course_id", "learner_id"}
        ),
        indexes = {
                @Index(name = "idx_course_enrollment_learner_status", columnList = "learner_id,status"),
                @Index(name = "idx_course_enrollment_course_status", columnList = "course_id,status")
        }
)
public class CourseEnrollment {

    @Id
    @Column(length = 80)
    private String id;

    @Column(name = "course_id", length = 80, nullable = false)
    private String courseId;

    @Column(name = "learner_id", length = 120, nullable = false)
    private String learnerId;

    @Column(length = 40, nullable = false)
    private String status = "ACTIVE";

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null || id.isBlank()) {
            id = "cen_" + UUID.randomUUID().toString().replace("-", "");
        }
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
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

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getLearnerId() {
        return learnerId;
    }

    public void setLearnerId(String learnerId) {
        this.learnerId = learnerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
