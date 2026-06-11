package com.learningos.analytics.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningos.analytics.domain.OpsAlertRecord;
import com.learningos.analytics.repository.OpsAlertRecordRepository;
import com.learningos.common.api.ErrorCode;
import com.learningos.common.exception.ApiException;
import com.learningos.config.OpsAlertProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OpsAlertPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(OpsAlertPersistenceService.class);
    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_ACKNOWLEDGED = "ACKNOWLEDGED";
    private static final String NOTIFICATION_SKIPPED = "SKIPPED";
    private static final String NOTIFICATION_SENT = "SENT";
    private static final String NOTIFICATION_FAILED = "FAILED";

    private final OpsAlertRecordRepository repository;
    private final OpsAlertProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpsAlertPersistenceService(
            OpsAlertRecordRepository repository,
            OpsAlertProperties properties,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    }

    @Transactional
    public AnalyticsService.OpsAlertItem persistTriggeredAlert(
            AnalyticsService.OpsAlertItem alert,
            Instant windowStart,
            Instant windowEnd
    ) {
        if (!properties.persistenceEnabled() || alert == null || !alert.triggered()) {
            return alert;
        }
        OpsAlertRecord record = repository
                .findByAlertTypeAndWindowStartAndWindowEnd(alert.type(), windowStart, windowEnd)
                .orElseGet(OpsAlertRecord::new);
        Instant now = Instant.now();
        if (record.getId() == null) {
            record.setId("alert_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            record.setCreatedAt(now);
            record.setStatus(STATUS_OPEN);
        }
        record.setAlertType(alert.type());
        record.setSeverity(alert.severity());
        record.setSummary(alert.summary());
        record.setMetricsJson(writeJson(alert.metrics()));
        record.setReasonCodesJson(writeJson(alert.reasonCodes()));
        record.setWindowStart(windowStart);
        record.setWindowEnd(windowEnd);
        record.setUpdatedAt(now);
        if (record.getNotificationStatus() == null) {
            record.setNotificationStatus(dispatchNotification(record));
        }
        repository.save(record);
        return new AnalyticsService.OpsAlertItem(
                alert.type(),
                alert.severity(),
                alert.triggered(),
                alert.count(),
                alert.threshold(),
                alert.summary(),
                alert.metrics(),
                alert.reasonCodes(),
                record.getId(),
                record.getStatus()
        );
    }

    @Transactional(readOnly = true)
    public List<AnalyticsService.PersistedOpsAlertRecord> recentRecords() {
        return repository.findTop50ByOrderByUpdatedAtDesc().stream()
                .map(this::toPersistedRecord)
                .toList();
    }

    @Transactional
    public AnalyticsService.PersistedOpsAlertRecord acknowledge(String alertId, String acknowledgedBy) {
        OpsAlertRecord record = repository.findById(requiredText(alertId, "Alert id is required"))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Ops alert not found"));
        record.setStatus(STATUS_ACKNOWLEDGED);
        record.setAcknowledgedBy(safeUserId(acknowledgedBy));
        record.setAcknowledgedAt(Instant.now());
        record.setUpdatedAt(Instant.now());
        return toPersistedRecord(repository.save(record));
    }

    private AnalyticsService.PersistedOpsAlertRecord toPersistedRecord(OpsAlertRecord record) {
        return new AnalyticsService.PersistedOpsAlertRecord(
                record.getId(),
                record.getAlertType(),
                record.getSeverity(),
                record.getSummary(),
                readMap(record.getMetricsJson()),
                readStringList(record.getReasonCodesJson()),
                record.getWindowStart(),
                record.getWindowEnd(),
                record.getStatus(),
                record.getAcknowledgedBy(),
                record.getAcknowledgedAt(),
                record.getNotificationStatus(),
                record.getUpdatedAt()
        );
    }

    private String dispatchNotification(OpsAlertRecord record) {
        if (!properties.webhookEnabled() || properties.webhookUrl().isBlank()) {
            return NOTIFICATION_SKIPPED;
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("alertId", record.getId());
            payload.put("alertType", record.getAlertType());
            payload.put("severity", record.getSeverity());
            payload.put("summary", record.getSummary());
            payload.put("windowStart", record.getWindowStart());
            payload.put("windowEnd", record.getWindowEnd());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.webhookUrl()))
                    .timeout(Duration.ofSeconds(3))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(writeJson(payload), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return NOTIFICATION_SENT;
            }
            log.warn("Ops alert webhook returned status {}", response.statusCode());
            return NOTIFICATION_FAILED;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return NOTIFICATION_FAILED;
        } catch (Exception ex) {
            log.warn("Ops alert webhook dispatch failed: {}", ex.getMessage());
            return NOTIFICATION_FAILED;
        }
    }

    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private String requiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, message);
        }
        return value.trim();
    }

    private String safeUserId(String userId) {
        return userId == null || userId.isBlank() ? null : userId.trim();
    }
}
