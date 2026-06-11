package com.learningos.agent.application;

import com.learningos.agent.domain.PromptVersion;
import com.learningos.agent.dto.PromptVersionResponse;
import com.learningos.agent.dto.PromptVersionUpsertRequest;
import com.learningos.agent.repository.PromptVersionRepository;
import com.learningos.common.api.ErrorCode;
import com.learningos.common.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class PromptVersionService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final List<String> ALLOWED_STATUSES = List.of("ACTIVE", "INACTIVE", "DRAFT", "ARCHIVED");

    private final PromptVersionRepository promptVersionRepository;

    public PromptVersionService(PromptVersionRepository promptVersionRepository) {
        this.promptVersionRepository = promptVersionRepository;
    }

    @Transactional
    public PromptVersionResponse upsert(PromptVersionUpsertRequest request, boolean currentUserAdmin) {
        requireAdmin(currentUserAdmin);
        String code = requiredTrimmed(request.code(), "Prompt code is required");
        String version = requiredTrimmed(request.version(), "Prompt version is required");
        String promptText = requiredPromptText(request.promptText());
        String status = normalizeStatus(request.status());

        PromptVersion promptVersion = promptVersionRepository.findByCodeAndVersion(code, version)
                .orElseGet(PromptVersion::new);
        promptVersion.setCode(code);
        promptVersion.setVersion(version);
        promptVersion.setPromptText(promptText);
        promptVersion.setStatus(status);

        return PromptVersionResponse.from(promptVersionRepository.save(promptVersion));
    }

    @Transactional(readOnly = true)
    public PromptVersionResponse get(String code, String version, boolean currentUserAdmin, boolean currentUserTeacher) {
        requirePromptRead(currentUserAdmin, currentUserTeacher);
        return promptVersionRepository.findByCodeAndVersion(code, version)
                .map(promptVersion -> PromptVersionResponse.from(promptVersion, currentUserAdmin))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Prompt version not found"));
    }

    @Transactional(readOnly = true)
    public List<PromptVersionResponse> list(String code, boolean currentUserAdmin, boolean currentUserTeacher) {
        requirePromptRead(currentUserAdmin, currentUserTeacher);
        List<PromptVersion> promptVersions = code == null || code.isBlank()
                ? promptVersionRepository.findAllByOrderByCreatedAtAsc()
                : promptVersionRepository.findByCodeOrderByCreatedAtAsc(code.trim());
        return promptVersions.stream()
                .map(promptVersion -> PromptVersionResponse.from(promptVersion, currentUserAdmin))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<PromptVersionResponse> findActiveByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return promptVersionRepository.findByCodeAndStatusOrderByCreatedAtDesc(code.trim(), STATUS_ACTIVE)
                .stream()
                .findFirst()
                .map(PromptVersionResponse::from);
    }

    private void requireAdmin(boolean currentUserAdmin) {
        if (!currentUserAdmin) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Prompt version admin access required");
        }
    }

    private void requirePromptRead(boolean currentUserAdmin, boolean currentUserTeacher) {
        if (!currentUserAdmin && !currentUserTeacher) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Prompt version access denied");
        }
    }

    private String requiredTrimmed(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, message);
        }
        return value.trim();
    }

    private String requiredPromptText(String value) {
        if (value == null || value.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Prompt text is required");
        }
        return value;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return STATUS_ACTIVE;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                    "Prompt status must be ACTIVE, INACTIVE, DRAFT or ARCHIVED");
        }
        return normalized;
    }
}
