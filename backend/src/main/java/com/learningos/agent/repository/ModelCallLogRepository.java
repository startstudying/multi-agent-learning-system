package com.learningos.agent.repository;

import com.learningos.agent.domain.ModelCallLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModelCallLogRepository extends JpaRepository<ModelCallLog, String> {

    long countByTraceId(String traceId);
}
