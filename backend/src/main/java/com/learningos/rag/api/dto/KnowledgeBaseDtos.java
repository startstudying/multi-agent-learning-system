package com.learningos.rag.api.dto;

import com.learningos.rag.domain.KnowledgeBase;
import com.learningos.rag.domain.enums.KnowledgeBaseBindingStatus;
import com.learningos.rag.domain.enums.Visibility;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public final class KnowledgeBaseDtos {

    private KnowledgeBaseDtos() {
    }

    public record CreateKnowledgeBaseRequest(
            @NotBlank String name,
            String description,
            Visibility visibility,
            String courseId
    ) {
    }

    public record KnowledgeBaseResponse(
            String id,
            String name,
            String description,
            Visibility visibility,
            String ownerUserId,
            String courseId,
            KnowledgeBaseBindingStatus bindingStatus,
            Instant boundAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static KnowledgeBaseResponse from(KnowledgeBase knowledgeBase) {
            return new KnowledgeBaseResponse(
                    knowledgeBase.getId(),
                    knowledgeBase.getName(),
                    knowledgeBase.getDescription(),
                    knowledgeBase.getVisibility(),
                    knowledgeBase.getOwnerUserId(),
                    knowledgeBase.getCourseId(),
                    knowledgeBase.getBindingStatus(),
                    knowledgeBase.getBoundAt(),
                    knowledgeBase.getCreatedAt(),
                    knowledgeBase.getUpdatedAt()
            );
        }
    }
}
