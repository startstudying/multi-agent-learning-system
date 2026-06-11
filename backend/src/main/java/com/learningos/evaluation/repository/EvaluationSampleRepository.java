package com.learningos.evaluation.repository;

import com.learningos.evaluation.domain.EvaluationSample;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationSampleRepository extends JpaRepository<EvaluationSample, String> {

    List<EvaluationSample> findBySetIdOrderByCreatedAtAsc(String setId);

    long countBySetId(String setId);

    void deleteBySetId(String setId);
}
