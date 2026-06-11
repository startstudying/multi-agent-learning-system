package com.learningos.rag.repository;

import com.learningos.rag.domain.KnowledgeBase;
import com.learningos.rag.domain.enums.Visibility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, String> {

    List<KnowledgeBase> findByDeletedAtIsNullAndOwnerUserIdOrDeletedAtIsNullAndVisibility(
            String ownerUserId,
            Visibility visibility
    );

    List<KnowledgeBase> findByIdInAndDeletedAtIsNull(Collection<String> ids);
}
