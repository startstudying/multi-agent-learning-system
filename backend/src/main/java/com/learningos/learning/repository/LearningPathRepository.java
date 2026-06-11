package com.learningos.learning.repository;

import com.learningos.learning.domain.LearningPath;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningPathRepository extends JpaRepository<LearningPath, String> {
}
