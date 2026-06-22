package com.learningos.aiqa.application.memory;

import com.learningos.aiqa.api.dto.AiQaDtos.AiQaRequest;
import com.learningos.aiqa.api.dto.AiQaDtos.AiQaResponse;
import com.learningos.aiqa.api.dto.AiQaDtos.MemoryMessageResponse;
import com.learningos.aiqa.api.dto.AiQaDtos.MemorySessionResponse;
import com.learningos.common.api.ErrorCode;
import com.learningos.common.exception.ApiException;
import com.learningos.common.privacy.MemoryPrivacyPolicy;
import com.learningos.rag.domain.KbChatMessage;
import com.learningos.rag.domain.KbChatSession;
import com.learningos.rag.repository.KbChatMessageRepository;
import com.learningos.rag.repository.KbChatSessionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MemoryLifecycleService {

    private static final int MAX_SESSIONS = 20;
    private static final int MAX_SUMMARY_LENGTH = 2000;
    private static final String DEFAULT_STATUS = "ACTIVE";
    private static final String DEFAULT_ROLE = "AI_QA_SUMMARY";

    private final KbChatSessionRepository sessionRepository;
    private final KbChatMessageRepository messageRepository;
    private final MemoryPrivacyPolicy privacyPolicy;

    public MemoryLifecycleService(
            KbChatSessionRepository sessionRepository,
            KbChatMessageRepository messageRepository,
            MemoryPrivacyPolicy privacyPolicy
    ) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.privacyPolicy = privacyPolicy;
    }

    @Transactional
    public KbChatMessage recordQaTurn(String learnerId, AiQaRequest request, AiQaResponse response) {
        String owner = requiredText(learnerId, "Learner id is required");
        Instant now = Instant.now();
        String sourcePolicy = valueOrDefault(response == null ? null : response.sourcePolicy(), "UNKNOWN");
        String verification = response == null || response.verification() == null
                ? "UNKNOWN"
                : valueOrDefault(response.verification().verdict(), "UNKNOWN");
        int citationCount = response == null || response.citations() == null ? 0 : response.citations().size();
        double salience = salienceScore(sourcePolicy, verification, citationCount, response != null && response.requiresReview());
        Instant decayAt = decayAt(now, salience, response != null && response.requiresReview(), sourcePolicy);

        KbChatSession session = findOrCreateSession(owner, request, sourcePolicy, salience, decayAt, now);
        KbChatMessage message = new KbChatMessage();
        message.setId("memm_" + UUID.randomUUID().toString().replace("-", ""));
        message.setSessionId(session.getId());
        message.setLearnerId(owner);
        message.setRole(DEFAULT_ROLE);
        message.setContentSummary(qaSummary(request, response, sourcePolicy, verification, citationCount));
        message.setSourcePolicy(sourcePolicy);
        message.setSalienceScore(salience);
        message.setDecayAt(decayAt);
        message.setEditable(true);
        message.setCreatedAt(now);
        message.setUpdatedAt(now);
        return messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public List<MemorySessionResponse> listSessions(String learnerId) {
        String owner = requiredText(learnerId, "Learner id is required");
        Instant now = Instant.now();
        List<KbChatSession> sessions = sessionRepository.findByLearnerIdAndDeletedAtIsNullOrderByUpdatedAtDesc(
                        owner,
                        PageRequest.of(0, MAX_SESSIONS, Sort.by(Sort.Direction.DESC, "updatedAt"))
                )
                .getContent();
        if (sessions.isEmpty()) {
            return List.of();
        }

        List<String> sessionIds = sessions.stream().map(KbChatSession::getId).toList();
        Map<String, List<KbChatMessage>> messagesBySession = messageRepository
                .findBySessionIdInAndLearnerIdAndDeletedAtIsNullAndDecayAtAfterOrderByCreatedAtAsc(sessionIds, owner, now)
                .stream()
                .collect(Collectors.groupingBy(KbChatMessage::getSessionId, LinkedHashMap::new, Collectors.toList()));

        return sessions.stream()
                .map(session -> toSessionResponse(session, messagesBySession.getOrDefault(session.getId(), List.of())))
                .toList();
    }

    @Transactional
    public KbChatMessage updateMessageSummary(String learnerId, String messageId, String summary) {
        KbChatMessage message = findOwnedActiveMessage(learnerId, messageId);
        if (!message.isEditable()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Memory message is not editable");
        }
        message.setContentSummary(sanitizeUserSummary(summary));
        message.setUpdatedAt(Instant.now());
        return messageRepository.save(message);
    }

    @Transactional
    public KbChatMessage deleteMessage(String learnerId, String messageId) {
        KbChatMessage message = findOwnedActiveMessage(learnerId, messageId);
        Instant now = Instant.now();
        message.setDeletedAt(now);
        message.setUpdatedAt(now);
        return messageRepository.save(message);
    }

    public MemoryMessageResponse toMessageResponse(KbChatMessage message) {
        return new MemoryMessageResponse(
                message.getId(),
                message.getContentSummary(),
                message.getSourcePolicy(),
                message.isEditable(),
                scoreOrDefault(message.getSalienceScore()),
                message.getDecayAt(),
                message.getDeletedAt() != null,
                message.getCreatedAt(),
                message.getUpdatedAt()
        );
    }

    private KbChatSession findOrCreateSession(
            String learnerId,
            AiQaRequest request,
            String sourcePolicy,
            double salience,
            Instant decayAt,
            Instant now
    ) {
        String sessionId = sessionId(request);
        KbChatSession session = sessionRepository.findById(sessionId)
                .filter(existing -> learnerId.equals(existing.getLearnerId()))
                .orElseGet(() -> newSession(sessionId, learnerId, request, sourcePolicy, now));
        session.setSalienceScore(Math.max(scoreOrDefault(session.getSalienceScore()), salience));
        session.setDecayAt(later(session.getDecayAt(), decayAt));
        session.setUpdatedAt(now);
        return sessionRepository.save(session);
    }

    private KbChatSession newSession(String sessionId, String learnerId, AiQaRequest request, String sourcePolicy, Instant now) {
        KbChatSession session = new KbChatSession();
        session.setId(sessionId);
        session.setLearnerId(learnerId);
        session.setCourseId(request == null ? null : blankToNull(request.courseId()));
        session.setTitle(safeTitle("AI QA " + valueOrDefault(sourcePolicy, "UNKNOWN")));
        session.setStatus(DEFAULT_STATUS);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        return session;
    }

    private String qaSummary(
            AiQaRequest request,
            AiQaResponse response,
            String sourcePolicy,
            String verification,
            int citationCount
    ) {
        String questionValue = privacyPolicy.questionLogValue(request == null ? null : request.question());
        int answerLength = normalize(response == null ? null : response.answer()).length();
        return truncate(questionValue
                + ";answerLength=" + answerLength
                + ";sourcePolicy=" + valueOrDefault(sourcePolicy, "UNKNOWN")
                + ";verification=" + valueOrDefault(verification, "UNKNOWN")
                + ";citationCount=" + citationCount, MAX_SUMMARY_LENGTH);
    }

    private MemorySessionResponse toSessionResponse(KbChatSession session, List<KbChatMessage> messages) {
        return new MemorySessionResponse(
                session.getId(),
                session.getCourseId(),
                session.getTitle(),
                session.getStatus(),
                scoreOrDefault(session.getSalienceScore()),
                session.getDecayAt(),
                messages.stream().map(this::toMessageResponse).toList()
        );
    }

    private KbChatMessage findOwnedActiveMessage(String learnerId, String messageId) {
        return messageRepository.findByIdAndLearnerIdAndDeletedAtIsNull(
                        requiredText(messageId, "Memory message id is required"),
                        requiredText(learnerId, "Learner id is required")
                )
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Memory message not found"));
    }

    private String sessionId(AiQaRequest request) {
        if (request != null && hasText(request.requestId())) {
            return "mems_" + safeReference(request.requestId(), 64);
        }
        return "mems_" + UUID.randomUUID().toString().replace("-", "");
    }

    private double salienceScore(String sourcePolicy, String verification, int citationCount, boolean requiresReview) {
        double score = 0.45;
        if ("COURSE_RAG".equalsIgnoreCase(sourcePolicy)) {
            score += 0.18;
        }
        if ("PASS".equalsIgnoreCase(verification)) {
            score += 0.18;
        }
        if (citationCount > 0) {
            score += Math.min(0.14, citationCount * 0.07);
        }
        if (requiresReview) {
            score -= 0.20;
        }
        if ("NO_COURSE_SOURCE_FALLBACK".equalsIgnoreCase(sourcePolicy)) {
            score -= 0.15;
        }
        return clamp(score);
    }

    private Instant decayAt(Instant now, double salience, boolean requiresReview, String sourcePolicy) {
        if (requiresReview || "NO_COURSE_SOURCE_FALLBACK".equalsIgnoreCase(sourcePolicy)) {
            return now.plus(Duration.ofDays(14));
        }
        if (salience >= 0.80) {
            return now.plus(Duration.ofDays(90));
        }
        if (salience >= 0.60) {
            return now.plus(Duration.ofDays(45));
        }
        return now.plus(Duration.ofDays(21));
    }

    private String sanitizeUserSummary(String summary) {
        String normalized = requiredText(summary, "Memory summary is required");
        if (isSensitiveMarker(normalized)) {
            return "[redacted-sensitive-memory]";
        }
        return truncate(normalized, MAX_SUMMARY_LENGTH);
    }

    private String safeTitle(String title) {
        String normalized = normalize(title);
        if (!hasText(normalized) || isSensitiveMarker(normalized)) {
            return "AI QA memory";
        }
        return truncate(normalized, 255);
    }

    private String safeReference(String value, int maxLength) {
        String normalized = normalize(value);
        if (!hasText(normalized) || isSensitiveMarker(normalized)) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        StringBuilder builder = new StringBuilder(Math.min(normalized.length(), maxLength));
        for (int i = 0; i < normalized.length() && builder.length() < maxLength; i++) {
            char item = normalized.charAt(i);
            if (Character.isLetterOrDigit(item) || item == '_' || item == '-' || item == ':' || item == '.') {
                builder.append(item);
            } else {
                builder.append('_');
            }
        }
        return builder.isEmpty() ? UUID.randomUUID().toString().replace("-", "") : builder.toString();
    }

    private Instant later(Instant current, Instant candidate) {
        if (current == null) {
            return candidate;
        }
        if (candidate == null) {
            return current;
        }
        return current.isAfter(candidate) ? current : candidate;
    }

    private String requiredText(String value, String message) {
        if (!hasText(value)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, message);
        }
        return value.trim();
    }

    private String valueOrDefault(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private String truncate(String value, int maxLength) {
        String normalized = normalize(value);
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private boolean isSensitiveMarker(String value) {
        if (!hasText(value)) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.contains("teacher_note")
                || normalized.contains("teacher note")
                || normalized.contains("provider_key")
                || normalized.contains("provider key")
                || normalized.contains("api_key")
                || normalized.contains("api key")
                || normalized.contains("sk-")
                || normalized.contains("secret")
                || normalized.contains("raw prompt")
                || normalized.contains("profilesnapshot")
                || normalized.contains("chain-of-thought");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private double scoreOrDefault(Double value) {
        return value == null ? 0.0 : clamp(value);
    }

    private double clamp(double value) {
        if (Double.isNaN(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }
}
