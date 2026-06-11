package com.learningos.assessment.repository;

import com.learningos.assessment.domain.AnswerRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface AnswerRecordRepository extends JpaRepository<AnswerRecord, String> {

    long countByLearnerId(String learnerId);

    Optional<AnswerRecord> findByLearnerIdAndRequestId(String learnerId, String requestId);

    Page<AnswerRecord> findByLearnerId(String learnerId, Pageable pageable);

    Page<AnswerRecord> findByLearnerIdAndQuestionIdIn(
            String learnerId,
            Collection<String> questionIds,
            Pageable pageable
    );

    Page<AnswerRecord> findByLearnerIdInAndQuestionIdIn(
            Collection<String> learnerIds,
            Collection<String> questionIds,
            Pageable pageable
    );

    Page<AnswerRecord> findByQuestionIdIn(Collection<String> questionIds, Pageable pageable);
}
