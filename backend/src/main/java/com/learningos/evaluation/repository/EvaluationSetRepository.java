package com.learningos.evaluation.repository;

import com.learningos.evaluation.domain.EvaluationSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluationSetRepository extends JpaRepository<EvaluationSet, String> {

    Optional<EvaluationSet> findByCreatedByAndCodeAndVersionAndDeletedAtIsNull(
            String createdBy,
            String code,
            String version
    );

    List<EvaluationSet> findByDeletedAtIsNullOrderByCreatedAtAsc();

    List<EvaluationSet> findByTypeAndDeletedAtIsNullOrderByCreatedAtAsc(String type);
}
