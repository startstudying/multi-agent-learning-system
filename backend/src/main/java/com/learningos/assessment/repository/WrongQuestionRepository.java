package com.learningos.assessment.repository;

import com.learningos.assessment.domain.WrongQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface WrongQuestionRepository extends JpaRepository<WrongQuestion, String> {

    long countByLearnerId(String learnerId);

    Optional<WrongQuestion> findFirstByAnswerIdOrderByCreatedAtDesc(String answerId);

    Page<WrongQuestion> findByLearnerId(String learnerId, Pageable pageable);

    Page<WrongQuestion> findByLearnerIdAndKnowledgePointIdIn(
            String learnerId,
            Collection<String> knowledgePointIds,
            Pageable pageable
    );

    Page<WrongQuestion> findByLearnerIdInAndKnowledgePointIdIn(
            Collection<String> learnerIds,
            Collection<String> knowledgePointIds,
            Pageable pageable
    );

    Page<WrongQuestion> findByKnowledgePointIdIn(Collection<String> knowledgePointIds, Pageable pageable);
}
