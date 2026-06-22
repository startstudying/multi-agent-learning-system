package com.learningos.rag.repository;

import com.learningos.rag.domain.KbChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface KbChatMessageRepository extends JpaRepository<KbChatMessage, String> {

    Optional<KbChatMessage> findByIdAndLearnerIdAndDeletedAtIsNull(String id, String learnerId);

    Page<KbChatMessage> findByLearnerIdAndDeletedAtIsNullAndDecayAtAfterOrderByCreatedAtDesc(
            String learnerId,
            Instant now,
            Pageable pageable
    );

    List<KbChatMessage> findBySessionIdInAndLearnerIdAndDeletedAtIsNullAndDecayAtAfterOrderByCreatedAtAsc(
            Collection<String> sessionIds,
            String learnerId,
            Instant now
    );
}
