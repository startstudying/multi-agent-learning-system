package com.learningos.rag.repository;

import com.learningos.rag.domain.KbChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KbChatSessionRepository extends JpaRepository<KbChatSession, String> {

    Optional<KbChatSession> findByIdAndLearnerIdAndDeletedAtIsNull(String id, String learnerId);

    Page<KbChatSession> findByLearnerIdAndDeletedAtIsNullOrderByUpdatedAtDesc(String learnerId, Pageable pageable);
}
