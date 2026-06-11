package com.learningos.rag.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningos.common.api.ErrorCode;
import com.learningos.common.exception.ApiException;
import com.learningos.common.observability.LearningOsMetrics;
import com.learningos.common.trace.TraceContext;
import com.learningos.rag.api.dto.RagQueryDtos.RagQueryResponse;
import com.learningos.rag.api.dto.RagQueryDtos.RetrievalMetadata;
import com.learningos.rag.api.dto.RagQueryDtos.SourceCitation;
import com.learningos.rag.domain.KbDocChunk;
import com.learningos.rag.domain.KbQueryLog;
import com.learningos.rag.domain.SourceCitationRecord;
import com.learningos.rag.repository.KbQueryLogRepository;
import com.learningos.rag.repository.SourceCitationRepository;
import com.learningos.safety.application.ContentSafetyService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RagQueryService {

    private static final int MAX_REQUEST_ID_LENGTH = 120;

    private final PermissionService permissionService;
    private final ChunkService chunkService;
    private final RerankerService rerankerService;
    private final AdaptiveRagRouter adaptiveRagRouter;
    private final ContentSafetyService contentSafetyService;
    private final KbQueryLogRepository queryLogRepository;
    private final SourceCitationRepository sourceCitationRepository;
    private final ObjectMapper objectMapper;
    private final LearningOsMetrics metrics;

    public RagQueryService(
            PermissionService permissionService,
            ChunkService chunkService,
            RerankerService rerankerService,
            AdaptiveRagRouter adaptiveRagRouter,
            ContentSafetyService contentSafetyService,
            KbQueryLogRepository queryLogRepository,
            SourceCitationRepository sourceCitationRepository,
            ObjectMapper objectMapper,
            LearningOsMetrics metrics
    ) {
        this.permissionService = permissionService;
        this.chunkService = chunkService;
        this.rerankerService = rerankerService;
        this.adaptiveRagRouter = adaptiveRagRouter;
        this.contentSafetyService = contentSafetyService;
        this.queryLogRepository = queryLogRepository;
        this.sourceCitationRepository = sourceCitationRepository;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
    }

    @Transactional
    public RagQueryResponse query(String userId, List<String> requestedKbIds, String question, Integer topK) {
        return query(userId, false, false, requestedKbIds, question, topK);
    }

    @Transactional
    public RagQueryResponse query(
            String userId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            List<String> requestedKbIds,
            String question,
            Integer topK
    ) {
        String traceId = TraceContext.currentTraceId().orElse("trc_" + UUID.randomUUID().toString().replace("-", ""));
        return query(userId, currentUserAdmin, currentUserTeacher, requestedKbIds, question, topK, traceId);
    }

    @Transactional
    public RagQueryResponse queryWithTraceId(
            String userId,
            List<String> requestedKbIds,
            String question,
            Integer topK,
            String traceId
    ) {
        return queryWithTraceId(userId, false, false, requestedKbIds, question, topK, traceId);
    }

    @Transactional
    public RagQueryResponse queryWithTraceId(
            String userId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            List<String> requestedKbIds,
            String question,
            Integer topK,
            String traceId
    ) {
        String resolvedTraceId = traceId == null || traceId.isBlank()
                ? TraceContext.currentTraceId().orElse("trc_" + UUID.randomUUID().toString().replace("-", ""))
                : traceId.trim();
        return query(userId, currentUserAdmin, currentUserTeacher, requestedKbIds, question, topK, resolvedTraceId);
    }

    @Transactional
    public RagQueryResponse queryWithRequestId(
            String userId,
            List<String> requestedKbIds,
            String question,
            Integer topK,
            String requestId
    ) {
        return queryWithRequestId(userId, false, false, requestedKbIds, question, topK, requestId);
    }

    @Transactional
    public RagQueryResponse queryWithRequestId(
            String userId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            List<String> requestedKbIds,
            String question,
            Integer topK,
            String requestId
    ) {
        String traceId = TraceContext.currentTraceId().orElse("trc_" + UUID.randomUUID().toString().replace("-", ""));
        return queryWithTraceIdAndRequestId(
                userId,
                currentUserAdmin,
                currentUserTeacher,
                requestedKbIds,
                question,
                topK,
                traceId,
                requestId
        );
    }

    @Transactional
    public RagQueryResponse queryWithTraceIdAndRequestId(
            String userId,
            List<String> requestedKbIds,
            String question,
            Integer topK,
            String traceId,
            String requestId
    ) {
        return queryWithTraceIdAndRequestId(userId, false, false, requestedKbIds, question, topK, traceId, requestId);
    }

    @Transactional
    public RagQueryResponse queryWithTraceIdAndRequestId(
            String userId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            List<String> requestedKbIds,
            String question,
            Integer topK,
            String traceId,
            String requestId
    ) {
        String resolvedTraceId = traceId == null || traceId.isBlank()
                ? TraceContext.currentTraceId().orElse("trc_" + UUID.randomUUID().toString().replace("-", ""))
                : traceId.trim();
        String normalizedRequestId = requireRequestId(requestId);
        try {
            ReplayContext replayContext = replayContext(
                    userId,
                    currentUserAdmin,
                    currentUserTeacher,
                    requestedKbIds,
                    question,
                    topK,
                    normalizedRequestId
            );
            Optional<KbQueryLog> existing = queryLogRepository.findByUserIdAndRequestId(userId, normalizedRequestId);
            if (existing.isPresent()) {
                return replayExistingQuery(existing.get(), replayContext.requestHash());
            }
            return queryResolved(
                    userId,
                    replayContext.allowedKbIds(),
                    replayContext.question(),
                    replayContext.limit(),
                    resolvedTraceId,
                    normalizedRequestId,
                    replayContext.requestHash()
            );
        } catch (DataIntegrityViolationException exception) {
            ReplayContext replayContext = replayContext(
                    userId,
                    currentUserAdmin,
                    currentUserTeacher,
                    requestedKbIds,
                    question,
                    topK,
                    normalizedRequestId
            );
            return replayConcurrentQuery(userId, normalizedRequestId, replayContext.requestHash(), exception);
        } catch (RuntimeException exception) {
            metrics.recordRagFailure("unknown", errorCode(exception));
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public Optional<RagQueryResponse> replayQueryIfPresent(
            String userId,
            List<String> requestedKbIds,
            String question,
            Integer topK,
            String requestId
    ) {
        return replayQueryIfPresent(userId, false, false, requestedKbIds, question, topK, requestId);
    }

    @Transactional(readOnly = true)
    public Optional<RagQueryResponse> replayQueryIfPresent(
            String userId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            List<String> requestedKbIds,
            String question,
            Integer topK,
            String requestId
    ) {
        try {
            String normalizedRequestId = requireRequestId(requestId);
            Optional<KbQueryLog> existing = queryLogRepository.findByUserIdAndRequestId(userId, normalizedRequestId);
            if (existing.isEmpty()) {
                return Optional.empty();
            }
            ReplayContext replayContext = replayContext(
                    userId,
                    currentUserAdmin,
                    currentUserTeacher,
                    requestedKbIds,
                    question,
                    topK,
                    normalizedRequestId
            );
            return Optional.of(replayExistingQuery(existing.get(), replayContext.requestHash()));
        } catch (RuntimeException exception) {
            metrics.recordRagFailure("unknown", errorCode(exception));
            throw exception;
        }
    }

    private RagQueryResponse query(
            String userId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            List<String> requestedKbIds,
            String question,
            Integer topK,
            String traceId
    ) {
        long startedAt = System.nanoTime();
        try {
            contentSafetyService.checkUserInput(question);

            List<String> allowedKbIds = permissionService.requireReadableKbIds(
                    userId,
                    currentUserAdmin,
                    currentUserTeacher,
                    requestedKbIds
            );
            if (allowedKbIds.isEmpty()) {
                throw new ApiException(ErrorCode.FORBIDDEN, "No accessible knowledge bases for this query");
            }

            int limit = topK == null || topK <= 0 ? 20 : topK;
            return queryResolved(userId, allowedKbIds, question, limit, traceId, null, null, startedAt);
        } catch (RuntimeException exception) {
            metrics.recordRagFailure("unknown", errorCode(exception));
            throw exception;
        }
    }

    private RagQueryResponse queryResolved(
            String userId,
            List<String> allowedKbIds,
            String question,
            int limit,
            String traceId,
            String requestId,
            String requestHash
    ) {
        return queryResolved(userId, allowedKbIds, question, limit, traceId, requestId, requestHash, System.nanoTime());
    }

    private RagQueryResponse queryResolved(
            String userId,
            List<String> allowedKbIds,
            String question,
            int limit,
            String traceId,
            String requestId,
            String requestHash,
            long startedAt
    ) {
        RetrievalResult retrievalResult = chunkService.retrieveAllowedChunks(allowedKbIds, question, limit);
        RerankResult rerankResult = rerankerService.rerankOrFallback(question, retrievalResult.chunks(), limit);
        List<KbDocChunk> chunks = rerankResult.chunks();
        List<SourceCitation> sources = chunks.stream()
                .map(this::toCitation)
                .toList();
        RetrievalMetadata retrieval = adaptiveRagRouter.describe(
                question,
                retrievalResult.fusedCandidateCount(),
                chunks.size(),
                sources.size(),
                rerankResult.fallbackUsed(),
                rerankResult.status().name()
        );
        String answer = retrieval.noSource()
                ? "No cited course material was found for the question: " + question
                : buildGroundedAnswer(chunks, question);
        RagQueryResponse response = new RagQueryResponse(answer, sources, traceId, retrieval);
        persistQueryLog(
                userId,
                allowedKbIds,
                question,
                chunks.size(),
                sources,
                retrieval,
                retrievalResult,
                rerankResult,
                traceId,
                startedAt,
                requestId,
                requestHash,
                response
        );
        if (!sources.isEmpty()) {
            persistSourceCitations(traceId, sources);
        }
        String outcome = retrieval.noSource() ? "no_source" : "success";
        long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        metrics.recordRagQuery(
                retrieval.strategy(),
                outcome,
                retrieval.noSource(),
                false,
                ErrorCode.OK.name(),
                latencyMs,
                retrieval.retrievalCount(),
                retrieval.citationCount()
        );
        return response;
    }

    private void persistQueryLog(
            String userId,
            List<String> allowedKbIds,
            String question,
            int retrievalCount,
            List<SourceCitation> sources,
            RetrievalMetadata retrieval,
            RetrievalResult retrievalResult,
            RerankResult rerankResult,
            String traceId,
            long startedAt,
            String requestId,
            String requestHash,
            RagQueryResponse response
    ) {
        KbQueryLog log = new KbQueryLog();
        DirectFieldAccessor fields = new DirectFieldAccessor(log);
        fields.setPropertyValue("traceId", traceId);
        fields.setPropertyValue("userId", userId);
        fields.setPropertyValue("kbIdsJson", toJson(allowedKbIds, 4000));
        fields.setPropertyValue("question", truncate(question, 4000));
        fields.setPropertyValue("requestId", requestId);
        fields.setPropertyValue("requestHash", requestHash);
        fields.setPropertyValue("responseJson", requestId == null ? null : toJson(response));
        fields.setPropertyValue("retrievalCount", retrievalCount);
        fields.setPropertyValue("rerankerStatus", rerankResult.status().name());
        fields.setPropertyValue("sourcesJson", toJson(queryLogSources(retrieval, retrievalResult, rerankResult, sources), 8000));
        fields.setPropertyValue("latencyMs", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
        queryLogRepository.saveAndFlush(log);
    }

    private Map<String, Object> queryLogSources(
            RetrievalMetadata retrieval,
            RetrievalResult retrievalResult,
            RerankResult rerankResult,
            List<SourceCitation> sources
    ) {
        return Map.of(
                "retrieval", retrieval,
                "hybrid", Map.of(
                        "retrievalMode", retrievalResult.retrievalMode(),
                        "rrfK", retrievalResult.rrfK(),
                        "keywordCandidateCount", retrievalResult.keywordCandidateCount(),
                        "recencyCandidateCount", retrievalResult.recencyCandidateCount(),
                        "vectorEnabled", retrievalResult.vectorEnabled(),
                        "vectorCandidateCount", retrievalResult.vectorCandidateCount(),
                        "fusedCandidateCount", retrievalResult.fusedCandidateCount()
                ),
                "reranker", Map.of(
                        "status", rerankResult.status().name(),
                        "fallbackUsed", rerankResult.fallbackUsed(),
                        "latencyMs", rerankResult.latencyMs() == null ? 0L : rerankResult.latencyMs(),
                        "errorCode", rerankResult.errorCode() == null ? "" : rerankResult.errorCode()
                ),
                "sources", sources.stream()
                        .map(source -> Map.of(
                                "documentId", source.documentId(),
                                "documentName", source.documentName(),
                                "pageNum", source.pageNum() == null ? 0 : source.pageNum(),
                                "sectionTitle", source.sectionTitle() == null ? "" : source.sectionTitle(),
                                "score", source.score()
                        ))
                        .toList()
        );
    }

    private void persistSourceCitations(String traceId, List<SourceCitation> sources) {
        sourceCitationRepository.saveAll(sources.stream()
                .map(source -> toSourceCitationRecord(traceId, source))
                .toList());
    }

    private SourceCitationRecord toSourceCitationRecord(String traceId, SourceCitation source) {
        SourceCitationRecord record = new SourceCitationRecord();
        record.setTraceId(traceId);
        record.setDocumentId(source.documentId());
        record.setDocumentName(source.documentName());
        record.setPageNum(source.pageNum());
        record.setSectionTitle(source.sectionTitle());
        record.setExcerpt(source.excerpt());
        record.setScore(source.score());
        return record;
    }

    private String toJson(Object value, int maxLength) {
        try {
            return truncate(objectMapper.writeValueAsString(value), maxLength);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize RAG query log", ex);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize RAG query response snapshot", ex);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String buildGroundedAnswer(List<KbDocChunk> chunks, String question) {
        String first = chunks.getFirst().getContent();
        return first.length() > 400 ? first.substring(0, 400) : first;
    }

    private SourceCitation toCitation(KbDocChunk chunk) {
        return new SourceCitation(
                chunk.getDocumentId(),
                chunk.getDocument() == null ? chunk.getDocumentId() : chunk.getDocument().getName(),
                chunk.getPageNum(),
                chunk.getSectionTitle(),
                excerpt(chunk.getContent()),
                1.0
        );
    }

    private ReplayContext replayContext(
            String userId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            List<String> requestedKbIds,
            String question,
            Integer topK,
            String requestId
    ) {
        String normalizedQuestion = question == null ? "" : question.trim();
        contentSafetyService.checkUserInput(normalizedQuestion);
        List<String> allowedKbIds = permissionService.requireReadableKbIds(
                userId,
                currentUserAdmin,
                currentUserTeacher,
                requestedKbIds
        );
        if (allowedKbIds.isEmpty()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "No accessible knowledge bases for this query");
        }
        int limit = topK == null || topK <= 0 ? 20 : topK;
        return new ReplayContext(
                normalizedQuestion,
                allowedKbIds,
                limit,
                requestHash(userId, allowedKbIds, normalizedQuestion, limit)
        );
    }

    private RagQueryResponse replayExistingQuery(KbQueryLog existingQuery, String requestHash) {
        DirectFieldAccessor fields = new DirectFieldAccessor(existingQuery);
        String existingRequestHash = (String) fields.getPropertyValue("requestHash");
        if (!requestHash.equals(existingRequestHash)) {
            throw new ApiException(ErrorCode.CONFLICT, "requestId already used with different RAG query payload");
        }
        String responseJson = (String) fields.getPropertyValue("responseJson");
        if (responseJson == null || responseJson.isBlank()) {
            throw new ApiException(ErrorCode.CONFLICT, "requestId is already being processed");
        }
        try {
            RagQueryResponse response = objectMapper.readValue(responseJson, RagQueryResponse.class);
            RetrievalMetadata retrieval = response.retrieval();
            metrics.recordRagQuery(
                    retrieval == null ? "unknown" : retrieval.strategy(),
                    "replay",
                    retrieval != null && retrieval.noSource(),
                    true,
                    ErrorCode.OK.name(),
                    null,
                    null,
                    null
            );
            return response;
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize RAG response snapshot", ex);
        }
    }

    private RagQueryResponse replayConcurrentQuery(
            String userId,
            String requestId,
            String requestHash,
            DataIntegrityViolationException originalException
    ) {
        return queryLogRepository.findByUserIdAndRequestId(userId, requestId)
                .map(existingQuery -> replayExistingQuery(existingQuery, requestHash))
                .orElseThrow(() -> originalException);
    }

    private String requireRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "requestId is required");
        }
        String normalized = requestId.trim();
        if (normalized.length() > MAX_REQUEST_ID_LENGTH) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "requestId length must be less than or equal to 120");
        }
        return normalized;
    }

    private String requestHash(String userId, List<String> allowedKbIds, String question, int limit) {
        String payload = String.join("\n",
                userId == null ? "" : userId.trim(),
                String.join(",", normalizeKbIds(allowedKbIds)),
                question == null ? "" : question.trim(),
                Integer.toString(limit),
                "rag-query-v1"
        );
        return sha256(payload);
    }

    private List<String> normalizeKbIds(List<String> kbIds) {
        if (kbIds == null) {
            return List.of();
        }
        return kbIds.stream()
                .filter(kbId -> kbId != null && !kbId.isBlank())
                .map(String::trim)
                .distinct()
                .sorted()
                .toList();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte item : hash) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }

    private String errorCode(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.getErrorCode().name();
        }
        return ErrorCode.INTERNAL_ERROR.name();
    }

    private String excerpt(String content) {
        if (content == null) {
            return "";
        }
        return content.length() > 180 ? content.substring(0, 180) : content;
    }

    private record ReplayContext(
            String question,
            List<String> allowedKbIds,
            int limit,
            String requestHash
    ) {
    }
}
