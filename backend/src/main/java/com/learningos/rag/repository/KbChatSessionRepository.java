package com.learningos.rag.repository;

import com.learningos.rag.domain.KbChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KbChatSessionRepository extends JpaRepository<KbChatSession, String> {
}
