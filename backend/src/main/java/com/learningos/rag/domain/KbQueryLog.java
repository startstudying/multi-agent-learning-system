package com.learningos.rag.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "kb_query_log",
        indexes = @Index(name = "idx_kb_query_log_trace_id", columnList = "trace_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_kb_query_user_request", columnNames = {"user_id", "request_id"})
)
public class KbQueryLog {
    @Id
    private String id;

    private String traceId;

    private String userId;

    @Column(columnDefinition = "text")
    private String kbIdsJson;

    @Column(columnDefinition = "text")
    private String question;

    @Column(length = 120)
    private String requestId;

    @Column(length = 128)
    private String requestHash;

    @Column(columnDefinition = "text")
    private String responseJson;

    private Integer retrievalCount;

    private String rerankerStatus;

    @Column(columnDefinition = "text")
    private String sourcesJson;

    private Long latencyMs;

    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) {
            id = "qry_" + UUID.randomUUID().toString().replace("-", "");
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
