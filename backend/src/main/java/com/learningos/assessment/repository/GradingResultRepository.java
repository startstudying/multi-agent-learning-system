package com.learningos.assessment.repository;

import com.learningos.assessment.domain.GradingResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GradingResultRepository extends JpaRepository<GradingResult, String> {

    Optional<GradingResult> findFirstByAnswerIdOrderByCreatedAtDesc(String answerId);
}
