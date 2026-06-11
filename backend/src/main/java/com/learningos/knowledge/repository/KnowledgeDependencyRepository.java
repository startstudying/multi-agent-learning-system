package com.learningos.knowledge.repository;

import com.learningos.knowledge.domain.KnowledgeDependency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface KnowledgeDependencyRepository extends JpaRepository<KnowledgeDependency, String> {

    List<KnowledgeDependency> findByKnowledgePointIdInOrPrerequisiteIdIn(
            Collection<String> knowledgePointIds,
            Collection<String> prerequisiteIds
    );
}
