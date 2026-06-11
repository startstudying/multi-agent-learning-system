package com.learningos.rag.repository;

import com.learningos.rag.domain.KbQueryLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KbQueryLogRepository extends JpaRepository<KbQueryLog, String> {

    Optional<KbQueryLog> findByUserIdAndRequestId(String userId, String requestId);
}
