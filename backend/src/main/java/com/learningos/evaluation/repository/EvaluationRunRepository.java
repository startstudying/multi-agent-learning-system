package com.learningos.evaluation.repository;

import com.learningos.evaluation.domain.EvaluationRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface EvaluationRunRepository extends JpaRepository<EvaluationRun, String> {

    List<EvaluationRun> findByEvaluationSetIdAndPromptCodeAndPromptVersionInAndStatusOrderByCreatedAtAsc(
            String evaluationSetId,
            String promptCode,
            Collection<String> promptVersions,
            String status
    );
}
