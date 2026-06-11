package com.learningos.analytics.repository;

import com.learningos.analytics.domain.OpsAlertRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OpsAlertRecordRepository extends JpaRepository<OpsAlertRecord, String> {

    Optional<OpsAlertRecord> findByAlertTypeAndWindowStartAndWindowEnd(
            String alertType,
            Instant windowStart,
            Instant windowEnd
    );

    List<OpsAlertRecord> findTop50ByOrderByUpdatedAtDesc();
}
