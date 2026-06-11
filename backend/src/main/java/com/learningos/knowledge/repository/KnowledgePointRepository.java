package com.learningos.knowledge.repository;

import com.learningos.knowledge.domain.KnowledgePoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgePointRepository extends JpaRepository<KnowledgePoint, String> {

    List<KnowledgePoint> findByCourseIdOrderByCreatedAtAsc(String courseId);
}
