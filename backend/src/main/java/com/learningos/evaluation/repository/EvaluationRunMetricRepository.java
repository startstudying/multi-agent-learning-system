package com.learningos.evaluation.repository;

import com.learningos.evaluation.domain.EvaluationRunMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface EvaluationRunMetricRepository extends JpaRepository<EvaluationRunMetric, String> {

    List<EvaluationRunMetric> findByRunIdOrderByCreatedAtAsc(String runId);

    List<EvaluationRunMetric> findByRunIdIn(Collection<String> runIds);
}
