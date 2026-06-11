package com.learningos.agent.repository;

import com.learningos.agent.domain.PromptVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PromptVersionRepository extends JpaRepository<PromptVersion, String> {

    Optional<PromptVersion> findByCodeAndVersion(String code, String version);

    List<PromptVersion> findByCodeOrderByCreatedAtAsc(String code);

    List<PromptVersion> findByCodeAndStatusOrderByCreatedAtDesc(String code, String status);

    List<PromptVersion> findAllByOrderByCreatedAtAsc();
}
