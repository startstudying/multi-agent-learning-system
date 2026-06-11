package com.learningos.rag.repository;

import com.learningos.rag.domain.SourceCitationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SourceCitationRepository extends JpaRepository<SourceCitationRecord, String> {

    long countByTraceId(String traceId);

    List<SourceCitationRecord> findByTraceIdOrderByCreatedAtAsc(String traceId);
}
