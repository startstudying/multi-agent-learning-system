package com.learningos.learning.repository;

import com.learningos.learning.domain.LearningEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningEventRepository extends JpaRepository<LearningEvent, String> {

    long countByLearnerId(String learnerId);

    Page<LearningEvent> findByLearnerId(String learnerId, Pageable pageable);
}
