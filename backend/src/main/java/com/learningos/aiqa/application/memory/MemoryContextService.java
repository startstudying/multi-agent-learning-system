package com.learningos.aiqa.application.memory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningos.aiqa.application.memory.MemoryContext.ContextInjectionReason;
import com.learningos.aiqa.application.memory.MemoryContext.LearnerSummary;
import com.learningos.aiqa.application.memory.MemoryContext.LearningSignal;
import com.learningos.aiqa.application.memory.MemoryContext.PreferenceMemory;
import com.learningos.aiqa.application.memory.MemoryContext.RagCitationContext;
import com.learningos.aiqa.application.memory.MemoryContext.RecentSessionSummary;
import com.learningos.aiqa.application.memory.MemoryContext.TokenBudget;
import com.learningos.assessment.domain.WrongQuestion;
import com.learningos.assessment.repository.WrongQuestionRepository;
import com.learningos.common.privacy.MemoryPrivacyPolicy;
import com.learningos.learning.domain.LearnerProfile;
import com.learningos.learning.domain.LearningEvent;
import com.learningos.learning.domain.MasteryRecord;
import com.learningos.learning.repository.LearnerProfileRepository;
import com.learningos.learning.repository.LearningEventRepository;
import com.learningos.learning.repository.MasteryRecordRepository;
import com.learningos.rag.api.dto.RagQueryDtos.RagQueryResponse;
import com.learningos.rag.api.dto.RagQueryDtos.SourceCitation;
import com.learningos.rag.domain.KbChatMessage;
import com.learningos.rag.repository.KbChatMessageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class MemoryContextService {

    private static final int MAX_TOKENS = 1200;
    private static final int MAX_WEAK_POINTS = 5;
    private static final int MAX_PREFERENCES = 5;
    private static final int MAX_MASTERY_SIGNALS = 3;
    private static final int MAX_WRONG_QUESTION_SIGNALS = 2;
    private static final int MAX_CITATIONS = 3;
    private static final int MAX_RECENT_SESSIONS = 3;
    private static final int MAX_TEXT_LENGTH = 160;
    private static final int MAX_EXCERPT_LENGTH = 96;

    private static final List<String> PROFILE_TEXT_FIELDS = List.of(
            "target",
            "learning_goal",
            "goal",
            "knowledge_foundation",
            "knowledge_gaps",
            "cognitive_style",
            "learning_pace",
            "multimodal_preference",
            "resource_preference",
            "resourcePreference",
            "pace"
    );

    private final LearnerProfileRepository learnerProfileRepository;
    private final LearningEventRepository learningEventRepository;
    private final MasteryRecordRepository masteryRecordRepository;
    private final WrongQuestionRepository wrongQuestionRepository;
    private final KbChatMessageRepository kbChatMessageRepository;
    private final MemoryPrivacyPolicy privacyPolicy;
    private final ObjectMapper objectMapper;

    public MemoryContextService(
            LearnerProfileRepository learnerProfileRepository,
            LearningEventRepository learningEventRepository,
            MasteryRecordRepository masteryRecordRepository,
            WrongQuestionRepository wrongQuestionRepository,
            KbChatMessageRepository kbChatMessageRepository,
            MemoryPrivacyPolicy privacyPolicy,
            ObjectMapper objectMapper
    ) {
        this.learnerProfileRepository = learnerProfileRepository;
        this.learningEventRepository = learningEventRepository;
        this.masteryRecordRepository = masteryRecordRepository;
        this.wrongQuestionRepository = wrongQuestionRepository;
        this.kbChatMessageRepository = kbChatMessageRepository;
        this.privacyPolicy = privacyPolicy;
        this.objectMapper = objectMapper;
    }

    public MemoryContext build(String learnerId, String courseId, RagQueryResponse ragResponse, String answerMode) {
        BuildState state = new BuildState();
        Optional<LearnerProfile> profile = findProfile(learnerId);

        LearnerSummary learner = buildLearnerSummary(profile, state);
        List<LearningSignal> learningSignals = new ArrayList<>();
        learningSignals.addAll(buildMasterySignals(learnerId, state));
        learningSignals.addAll(buildWrongQuestionSignals(learnerId, state));

        List<RagCitationContext> citations = buildCitationContexts(ragResponse, state);
        List<RecentSessionSummary> recentSessions = buildRecentSessions(learnerId, state);
        List<PreferenceMemory> preferences = buildPreferences(profile, state);

        List<ContextInjectionReason> reasons = buildReasons(learner, learningSignals, citations, recentSessions, preferences, answerMode);
        int estimatedTokens = Math.min(MAX_TOKENS, estimateTokens(learner, learningSignals, citations, recentSessions, preferences, reasons));
        TokenBudget budget = new TokenBudget(
                MAX_TOKENS,
                estimatedTokens,
                Math.max(0, MAX_TOKENS - estimatedTokens),
                state.truncated
        );
        return new MemoryContext(
                learner,
                List.copyOf(learningSignals),
                List.copyOf(citations),
                List.copyOf(recentSessions),
                List.copyOf(preferences),
                List.copyOf(reasons),
                budget
        );
    }

    private Optional<LearnerProfile> findProfile(String learnerId) {
        if (!hasText(learnerId)) {
            return Optional.empty();
        }
        return learnerProfileRepository.findFirstByLearnerIdOrderByUpdatedAtDesc(learnerId);
    }

    private LearnerSummary buildLearnerSummary(Optional<LearnerProfile> profile, BuildState state) {
        if (profile.isEmpty()) {
            return new LearnerSummary(
                    privacyPolicy.profileRef(null),
                    "",
                    List.of(),
                    "learner_profile:none",
                    0.20,
                    "No learner profile was available; keep personalization minimal."
            );
        }
        LearnerProfile existing = profile.get();
        return new LearnerSummary(
                privacyPolicy.profileRef(existing.getId()),
                safeText(existing.getTarget(), MAX_TEXT_LENGTH, state),
                limitedStrings(readStrings(existing.getWeakPointsJson(), state), MAX_WEAK_POINTS, state),
                "learner_profile",
                0.86,
                "Use low-sensitive profile summary to adapt explanation level."
        );
    }

    private List<LearningSignal> buildMasterySignals(String learnerId, BuildState state) {
        if (!hasText(learnerId)) {
            return List.of();
        }
        return masteryRecordRepository.findByLearnerId(
                        learnerId,
                PageRequest.of(0, MAX_MASTERY_SIGNALS, Sort.by(Sort.Direction.DESC, "updatedAt"))
                )
                .getContent()
                .stream()
                .map(record -> new LearningSignal(
                        "MASTERY",
                        safeReference(record.getId(), "mastery"),
                        safeText("knowledgePoint=" + record.getKnowledgePointId()
                                + ";mastery=" + record.getMastery()
                                + ";reason=" + nullToBlank(record.getReasonSummary()), MAX_TEXT_LENGTH, state),
                        "mastery_record",
                        clamp(record.getMastery() == null ? 0.50 : 1.0 - Math.abs(0.70 - record.getMastery())),
                        "Use current mastery to choose depth and remediation."
                ))
                .toList();
    }

    private List<LearningSignal> buildWrongQuestionSignals(String learnerId, BuildState state) {
        if (!hasText(learnerId)) {
            return List.of();
        }
        return wrongQuestionRepository.findByLearnerId(
                        learnerId,
                PageRequest.of(0, MAX_WRONG_QUESTION_SIGNALS, Sort.by(Sort.Direction.DESC, "createdAt"))
                )
                .getContent()
                .stream()
                .map(question -> wrongQuestionSignal(question, state))
                .toList();
    }

    private LearningSignal wrongQuestionSignal(WrongQuestion question, BuildState state) {
        String summary = "knowledgePoint=" + question.getKnowledgePointId()
                + ";score=" + question.getScore()
                + ";cause=" + nullToBlank(question.getCauseAnalysis())
                + ";strategy=" + nullToBlank(question.getResourcePushStrategy());
        return new LearningSignal(
                "WRONG_QUESTION",
                safeReference(question.getId(), "wrong-question"),
                safeText(summary, MAX_TEXT_LENGTH, state),
                "wrong_question",
                clamp(question.getScore() == null ? 0.50 : 1.0 - question.getScore()),
                "Use recent mistakes to target misconceptions."
        );
    }

    private List<RagCitationContext> buildCitationContexts(RagQueryResponse ragResponse, BuildState state) {
        if (ragResponse == null || ragResponse.sources() == null || ragResponse.sources().isEmpty()) {
            return List.of();
        }
        List<SourceCitation> sources = ragResponse.sources();
        if (sources.size() > MAX_CITATIONS) {
            state.truncated = true;
        }
        return sources.stream()
                .limit(MAX_CITATIONS)
                .map(source -> new RagCitationContext(
                        safeReference(source.documentId(), "document"),
                        safeText(source.documentName(), MAX_TEXT_LENGTH, state),
                        source.pageNum(),
                        safeText(source.sectionTitle(), MAX_TEXT_LENGTH, state),
                        safeText(source.excerpt(), MAX_EXCERPT_LENGTH, state),
                        "rag_citation",
                        clamp(source.score()),
                        "Use course-grounded evidence returned by RAG."
                ))
                .toList();
    }

    private List<RecentSessionSummary> buildRecentSessions(String learnerId, BuildState state) {
        if (!hasText(learnerId)) {
            return List.of();
        }
        List<RecentSessionSummary> activeMemories = kbChatMessageRepository
                .findByLearnerIdAndDeletedAtIsNullAndDecayAtAfterOrderByCreatedAtDesc(
                        learnerId,
                        Instant.now(),
                        PageRequest.of(0, MAX_RECENT_SESSIONS, Sort.by(Sort.Direction.DESC, "createdAt"))
                )
                .getContent()
                .stream()
                .filter(message -> hasText(message.getContentSummary()))
                .map(message -> recentSession(message, state))
                .toList();
        if (!activeMemories.isEmpty()) {
            return activeMemories;
        }
        return learningEventRepository.findByLearnerId(
                        learnerId,
                PageRequest.of(0, MAX_RECENT_SESSIONS, Sort.by(Sort.Direction.DESC, "createdAt"))
                )
                .getContent()
                .stream()
                .filter(event -> hasText(event.getSummary()))
                .map(event -> recentSession(event, state))
                .toList();
    }

    private RecentSessionSummary recentSession(KbChatMessage message, BuildState state) {
        return new RecentSessionSummary(
                safeReference(message.getId(), "memory-message"),
                safeText(message.getContentSummary(), MAX_TEXT_LENGTH, state),
                "kb_chat_message",
                clamp(message.getSalienceScore() == null ? 0.50 : message.getSalienceScore()),
                "Use active user-governed QA memory before learning-event fallback."
        );
    }

    private RecentSessionSummary recentSession(LearningEvent event, BuildState state) {
        return new RecentSessionSummary(
                safeReference(event.getId(), "learning-event"),
                safeText(event.getSummary(), MAX_TEXT_LENGTH, state),
                "learning_event",
                scoreEvent(event),
                "Use recent learning summary as short-lived conversation context."
        );
    }

    private List<PreferenceMemory> buildPreferences(Optional<LearnerProfile> profile, BuildState state) {
        if (profile.isEmpty()) {
            return List.of();
        }
        List<PreferenceMemory> preferences = new ArrayList<>();
        LearnerProfile existing = profile.get();
        addPreferenceValues(preferences, "profile.preference", readStrings(existing.getPreferencesJson(), state), state);
        addPreferenceValues(preferences, "profile.dimension", readDimensionStrings(existing.getDimensionsJson(), state), state);
        if (preferences.size() > MAX_PREFERENCES) {
            state.truncated = true;
        }
        return preferences.stream().limit(MAX_PREFERENCES).toList();
    }

    private void addPreferenceValues(List<PreferenceMemory> preferences, String source, List<String> values, BuildState state) {
        for (String value : values) {
            String safe = safeText(value, MAX_TEXT_LENGTH, state);
            if (!hasText(safe)) {
                continue;
            }
            preferences.add(new PreferenceMemory(
                    source,
                    safe,
                    source,
                    0.74,
                    "Use learner preference only after privacy filtering."
            ));
        }
    }

    private List<ContextInjectionReason> buildReasons(
            LearnerSummary learner,
            List<LearningSignal> learningSignals,
            List<RagCitationContext> citations,
            List<RecentSessionSummary> recentSessions,
            List<PreferenceMemory> preferences,
            String answerMode
    ) {
        List<ContextInjectionReason> reasons = new ArrayList<>();
        if (learner != null) {
            reasons.add(new ContextInjectionReason("LEARNER", learner.source(), learner.score(), learner.reason()));
        }
        learningSignals.forEach(signal -> reasons.add(new ContextInjectionReason(
                signal.type(),
                signal.source(),
                signal.score(),
                signal.reason()
        )));
        citations.forEach(citation -> reasons.add(new ContextInjectionReason(
                "RAG_CITATION",
                citation.source(),
                citation.score(),
                citation.reason()
        )));
        recentSessions.forEach(session -> reasons.add(new ContextInjectionReason(
                "RECENT_SESSION",
                session.source(),
                session.score(),
                session.reason()
        )));
        preferences.forEach(preference -> reasons.add(new ContextInjectionReason(
                "PREFERENCE",
                preference.source(),
                preference.score(),
                preference.reason() + " answerMode=" + nullToBlank(answerMode)
        )));
        return reasons;
    }

    private int estimateTokens(
            LearnerSummary learner,
            List<LearningSignal> learningSignals,
            List<RagCitationContext> citations,
            List<RecentSessionSummary> recentSessions,
            List<PreferenceMemory> preferences,
            List<ContextInjectionReason> reasons
    ) {
        int chars = 0;
        if (learner != null) {
            chars += length(learner.profileRef()) + length(learner.target()) + length(learner.source()) + length(learner.reason());
            chars += learner.weakPoints().stream().mapToInt(this::length).sum();
        }
        chars += learningSignals.stream().mapToInt(signal ->
                length(signal.type()) + length(signal.referenceId()) + length(signal.summary()) + length(signal.source()) + length(signal.reason())
        ).sum();
        chars += citations.stream().mapToInt(citation ->
                length(citation.documentId()) + length(citation.documentName()) + length(citation.sectionTitle())
                        + length(citation.excerptSummary()) + length(citation.source()) + length(citation.reason())
        ).sum();
        chars += recentSessions.stream().mapToInt(session ->
                length(session.referenceId()) + length(session.summary()) + length(session.source()) + length(session.reason())
        ).sum();
        chars += preferences.stream().mapToInt(preference ->
                length(preference.key()) + length(preference.value()) + length(preference.source()) + length(preference.reason())
        ).sum();
        chars += reasons.stream().mapToInt(reason ->
                length(reason.contextType()) + length(reason.source()) + length(reason.reason())
        ).sum();
        return Math.min(MAX_TOKENS, Math.max(1, (int) Math.ceil(chars / 4.0)));
    }

    private List<String> readStrings(String json, BuildState state) {
        if (!hasText(json)) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            List<String> values = new ArrayList<>();
            collectStrings(root, values, state);
            return values;
        } catch (Exception ex) {
            state.truncated = true;
            return List.of(safeText(json, MAX_TEXT_LENGTH, state));
        }
    }

    private List<String> readDimensionStrings(String json, BuildState state) {
        if (!hasText(json)) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode dimensions = root.isArray() ? root : root.path("dimensions");
            if (!dimensions.isArray()) {
                return readStrings(json, state);
            }
            List<String> values = new ArrayList<>();
            for (JsonNode dimension : dimensions) {
                String name = text(dimension.path("name"));
                if (!isAllowedProfileField(name)) {
                    if (isSensitiveMarker(name)) {
                        state.truncated = true;
                    }
                    continue;
                }
                String value = text(dimension.path("value"));
                if (hasText(value)) {
                    values.add(name + "=" + value);
                }
            }
            return values;
        } catch (Exception ex) {
            state.truncated = true;
            return List.of();
        }
    }

    private void collectStrings(JsonNode node, List<String> values, BuildState state) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isTextual() || node.isNumber() || node.isBoolean()) {
            String value = node.asText();
            if (isSensitiveMarker(value)) {
                state.truncated = true;
                return;
            }
            values.add(value);
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                collectStrings(item, values, state);
            }
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = node.properties().iterator();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (isSensitiveMarker(field.getKey())) {
                state.truncated = true;
                continue;
            }
            if (isAllowedProfileField(field.getKey()) || field.getValue().isArray()) {
                collectStrings(field.getValue(), values, state);
            }
        }
    }

    private List<String> limitedStrings(List<String> values, int maxItems, BuildState state) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> safeValues = values.stream()
                .map(value -> safeText(value, MAX_TEXT_LENGTH, state))
                .filter(this::hasText)
                .limit(maxItems)
                .toList();
        if (values.size() > maxItems) {
            state.truncated = true;
        }
        return safeValues;
    }

    private String safeText(String value, int maxLength, BuildState state) {
        String normalized = normalize(value);
        if (!hasText(normalized)) {
            return "";
        }
        if (isSensitiveMarker(normalized)) {
            state.truncated = true;
            return "[redacted-sensitive-memory]";
        }
        if (normalized.length() > maxLength) {
            state.truncated = true;
            return normalized.substring(0, Math.max(0, maxLength - 3)) + "...";
        }
        return normalized;
    }

    private String safeReference(String value, String fallback) {
        String normalized = normalize(value);
        if (!hasText(normalized) || isSensitiveMarker(normalized)) {
            return fallback + ":none";
        }
        StringBuilder builder = new StringBuilder(Math.min(normalized.length(), 120));
        for (int i = 0; i < normalized.length() && builder.length() < 120; i++) {
            char item = normalized.charAt(i);
            if (Character.isLetterOrDigit(item) || item == '_' || item == '-' || item == ':' || item == '.' || item == '#') {
                builder.append(item);
            } else {
                builder.append('_');
            }
        }
        return builder.toString();
    }

    private boolean isAllowedProfileField(String field) {
        if (!hasText(field) || isSensitiveMarker(field)) {
            return false;
        }
        return PROFILE_TEXT_FIELDS.stream().anyMatch(allowed -> allowed.equalsIgnoreCase(field));
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
                || normalized.contains("secret");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        return node.asText("");
    }

    private int length(String value) {
        return value == null ? 0 : value.length();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private double clamp(double value) {
        if (Double.isNaN(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    private double scoreEvent(LearningEvent event) {
        return switch (event.getEventType() == null ? "" : event.getEventType()) {
            case "QA_SESSION_SUMMARY" -> 0.72;
            case "PROFILE_UPDATE" -> 0.68;
            default -> 0.55;
        };
    }

    private static final class BuildState {
        private boolean truncated;
    }
}
