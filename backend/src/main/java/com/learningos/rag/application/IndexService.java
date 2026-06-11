package com.learningos.rag.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningos.common.api.ErrorCode;
import com.learningos.common.exception.ApiException;
import com.learningos.rag.domain.KbDocChunk;
import com.learningos.rag.domain.KbDocument;
import com.learningos.rag.domain.KbIndexTask;
import com.learningos.rag.domain.enums.DocumentStatus;
import com.learningos.rag.domain.enums.IndexTaskStatus;
import com.learningos.rag.parser.DocumentParseException;
import com.learningos.rag.parser.DocumentParser;
import com.learningos.rag.parser.DocumentParserService;
import com.learningos.rag.parser.ParsedDocument;
import com.learningos.rag.parser.ParsedSection;
import com.learningos.rag.repository.KbDocChunkRepository;
import com.learningos.rag.repository.KbDocumentRepository;
import com.learningos.rag.repository.KbIndexTaskRepository;
import com.learningos.rag.storage.DocumentStorageService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class IndexService {

    private static final int MAX_CHUNK_LENGTH = 1000;
    private static final int MAX_CHUNK_TOKENS = 220;
    private static final int CHUNK_OVERLAP_TOKENS = 40;
    private static final String CHUNKING_STRATEGY = "TOKEN_WINDOW_V1";
    private static final int MANUAL_MAX_RETRY_COUNT = 0;
    private static final Duration MANUAL_RETRY_BACKOFF = Duration.ofSeconds(30);
    private static final Duration MANUAL_LEASE_DURATION = Duration.ofMinutes(2);

    private static final Set<IndexTaskStatus> ACTIVE_STATUSES = EnumSet.of(
            IndexTaskStatus.PENDING,
            IndexTaskStatus.RUNNING
    );

    private final KbDocumentRepository documentRepository;
    private final KbIndexTaskRepository indexTaskRepository;
    private final KbDocChunkRepository chunkRepository;
    private final DocumentStorageService storageService;
    private final DocumentParserService parserService;
    private final EmbeddingService embeddingService;
    private final VectorIndexAdapter vectorIndexAdapter;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate stageTransactionTemplate;

    public IndexService(
            KbDocumentRepository documentRepository,
            KbIndexTaskRepository indexTaskRepository,
            KbDocChunkRepository chunkRepository,
            DocumentStorageService storageService,
            DocumentParserService parserService,
            EmbeddingService embeddingService,
            VectorIndexAdapter vectorIndexAdapter,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager
    ) {
        this.documentRepository = documentRepository;
        this.indexTaskRepository = indexTaskRepository;
        this.chunkRepository = chunkRepository;
        this.storageService = storageService;
        this.parserService = parserService;
        this.embeddingService = embeddingService;
        this.vectorIndexAdapter = vectorIndexAdapter;
        this.objectMapper = objectMapper;
        this.stageTransactionTemplate = new TransactionTemplate(transactionManager);
        this.stageTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional
    public KbIndexTask createPendingTask(KbDocument document) {
        if (document == null || document.getId() == null || document.getId().isBlank()) {
            throw new IllegalArgumentException("document id is required");
        }
        KbDocument lockedDocument = documentRepository.findByIdAndDeletedAtIsNullForUpdate(document.getId())
                .orElse(document);
        KbIndexTask activeTask = indexTaskRepository.findFirstByDocumentIdOrderByCreatedAtDesc(lockedDocument.getId())
                .filter(task -> ACTIVE_STATUSES.contains(task.getStatus()))
                .orElse(null);
        if (activeTask != null) {
            return activeTask;
        }
        KbIndexTask task = new KbIndexTask();
        task.setDocumentId(lockedDocument.getId());
        task.setKbId(lockedDocument.getKbId());
        task.setStatus(IndexTaskStatus.PENDING);
        task.setProgressPercent(0);
        task.setProgressPhase("PENDING");
        task.setRecoverable(true);
        return indexTaskRepository.save(task);
    }

    @Transactional
    public KbIndexTask processIndexTask(String taskId) {
        return processIndexTask(taskId, null, MANUAL_MAX_RETRY_COUNT, MANUAL_RETRY_BACKOFF, MANUAL_LEASE_DURATION);
    }

    @Transactional
    public List<KbIndexTask> claimDuePendingTasks(
            Instant now,
            int batchSize,
            String workerId,
            Duration leaseDuration
    ) {
        if (batchSize <= 0) {
            return List.of();
        }
        String normalizedWorkerId = workerId == null || workerId.isBlank() ? "index-worker" : workerId.trim();
        Duration safeLeaseDuration = leaseDuration == null || leaseDuration.isNegative() || leaseDuration.isZero()
                ? Duration.ofMinutes(2)
                : leaseDuration;
        List<KbIndexTask> dueTasks = indexTaskRepository.findDuePendingTasksForUpdate(now, PageRequest.of(0, batchSize));
        for (KbIndexTask task : dueTasks) {
            markClaimed(task, normalizedWorkerId, now, safeLeaseDuration);
        }
        return dueTasks;
    }

    @Transactional
    public KbIndexTask processIndexTask(
            String taskId,
            String workerId,
            int maxRetryCount,
            Duration retryBackoff,
            Duration leaseDuration
    ) {
        KbIndexTask task = indexTaskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Index task not found"));
        if (task.getStatus() == IndexTaskStatus.SUCCEEDED || task.getStatus() == IndexTaskStatus.FAILED) {
            return task;
        }
        Instant startedAt = Instant.now();
        Duration safeLeaseDuration = leaseDuration == null || leaseDuration.isNegative() || leaseDuration.isZero()
                ? Duration.ofMinutes(2)
                : leaseDuration;
        markRunningStage(task, workerId, startedAt, safeLeaseDuration, "PARSING", 20);
        KbDocument document = null;

        try {
            document = documentRepository.findByIdAndDeletedAtIsNull(task.getDocumentId())
                    .orElseThrow(() -> new IndexingException("DOCUMENT_EMPTY_OR_UNAVAILABLE"));
            document.setParseStatus(DocumentStatus.PARSING);
            document.setIndexStatus(DocumentStatus.PARSING);
            document.setErrorMessage(null);
            byte[] bytes = storageService.read(document.getStorageBucket(), document.getStorageKey());
            if (bytes == null || bytes.length == 0) {
                throw new IndexingException("DOCUMENT_EMPTY_OR_UNAVAILABLE");
            }
            ParsedDocument parsedDocument = parserService.parse(document, bytes);
            document.setParseStatus(DocumentStatus.CHUNKING);
            document.setIndexStatus(DocumentStatus.CHUNKING);
            markRunningStage(task, workerId, Instant.now(), safeLeaseDuration, "CHUNKING", 45);
            List<ChunkDraft> drafts = splitAndDeduplicate(parsedDocument);
            if (drafts.isEmpty()) {
                throw new IndexingException("DOCUMENT_EMPTY_OR_UNAVAILABLE");
            }
            EmbeddingBatchResult placeholderEmbedding = EmbeddingBatchResult.disabled(
                    embeddingService.currentModelVersion(),
                    drafts.size()
            );
            VectorUpsertResult placeholderVector = VectorUpsertResult.disabled();
            List<KbDocChunk> chunks = toChunks(
                    document,
                    parsedDocument.parser(),
                    drafts,
                    placeholderEmbedding,
                    placeholderVector
            );
            document.setIndexStatus(DocumentStatus.EMBEDDING);
            markRunningStage(task, workerId, Instant.now(), safeLeaseDuration, "EMBEDDING", 70);
            chunkRepository.deleteByDocumentId(document.getId());
            VectorUpsertResult deleteResult = deleteVectorDocument(document);
            ensureVectorUpsertSucceeded(deleteResult);
            EmbeddingBatchResult embeddingResult = embedDocumentChunks(document, chunks);
            ensureEmbeddingSucceeded(embeddingResult);
            markRunningStage(task, workerId, Instant.now(), safeLeaseDuration, "VECTOR_UPSERT", 85);
            VectorUpsertResult vectorResult = upsertVectorIndex(document, chunks, embeddingResult);
            ensureVectorUpsertSucceeded(vectorResult);
            applyIndexPipelineMetadata(chunks, embeddingResult, vectorResult);
            chunkRepository.saveAll(chunks);
            document.setParseStatus(DocumentStatus.INDEXED);
            document.setIndexStatus(DocumentStatus.INDEXED);
            task.setStatus(IndexTaskStatus.SUCCEEDED);
            task.setProgressPhase("SUCCEEDED");
            task.setProgressPercent(100);
            task.setHeartbeatAt(Instant.now());
            task.setLeaseOwner(null);
            task.setLeaseUntil(null);
            task.setNextRetryAt(null);
            task.setRecoverable(false);
            task.setFinishedAt(Instant.now());
            task.setErrorMessage(null);
            return task;
        } catch (RuntimeException | IOException exception) {
            markIndexFailed(task, document, exception, maxRetryCount, retryBackoff);
            return task;
        }
    }

    @Transactional
    public int recoverTimedOutRunningTasks(Instant timeoutCutoff) {
        return recoverTimedOutRunningTasks(timeoutCutoff, MANUAL_MAX_RETRY_COUNT, MANUAL_RETRY_BACKOFF);
    }

    @Transactional
    public int recoverTimedOutRunningTasks(Instant timeoutCutoff, int maxRetryCount, Duration retryBackoff) {
        var timedOutTasks = indexTaskRepository.findExpiredRunningTasksForUpdate(timeoutCutoff);
        for (KbIndexTask task : timedOutTasks) {
            markRetryOrTerminalFailure(task, "DOCUMENT_INDEX_LEASE_EXPIRED", maxRetryCount, retryBackoff);
        }
        return timedOutTasks.size();
    }

    private List<ChunkDraft> splitAndDeduplicate(ParsedDocument parsedDocument) {
        List<ChunkDraft> chunks = new ArrayList<>();
        for (ParsedSection section : parsedDocument.sections()) {
            List<String> tokens = tokens(section.content());
            if (tokens.isEmpty()) {
                continue;
            }
            int start = 0;
            while (start < tokens.size()) {
                int end = Math.min(tokens.size(), start + MAX_CHUNK_TOKENS);
                while (end > start + 1 && joinTokens(tokens, start, end).length() > MAX_CHUNK_LENGTH) {
                    end--;
                }
                String content = clean(joinTokens(tokens, start, end));
                if (!content.isBlank()) {
                    chunks.add(new ChunkDraft(
                            content,
                            section.title(),
                            section.headingLevel(),
                            section.headingPath(),
                            section.pageNum(),
                            section.pageNumSource(),
                            section.readingOrderIndex(),
                            section.layoutConfidence(),
                            section.ocrConfidence(),
                            section.contentKind(),
                            end - start
                    ));
                }
                if (end >= tokens.size()) {
                    break;
                }
                start = Math.max(end - CHUNK_OVERLAP_TOKENS, start + 1);
            }
        }
        return chunks;
    }

    private String metadataJson(
            DocumentParser parser,
            ChunkDraft draft,
            EmbeddingBatchResult embeddingResult,
            VectorUpsertResult vectorResult
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("parser", parser.name());
        metadata.put("embeddingModel", embeddingResult.modelVersion());
        metadata.put("embeddingStatus", embeddingResult.status().name());
        metadata.put("vectorIndexStatus", vectorResult.status().name());
        metadata.put("contentLength", draft.content().length());
        metadata.put("chunkingStrategy", CHUNKING_STRATEGY);
        metadata.put("chunkTokenCount", draft.tokenCount());
        metadata.put("overlapTokenCount", CHUNK_OVERLAP_TOKENS);
        metadata.put("headingLevel", draft.headingLevel());
        metadata.put("headingPath", draft.headingPath());
        metadata.put("pageNumSource", draft.pageNumSource());
        metadata.put("readingOrderIndex", draft.readingOrderIndex());
        metadata.put("contentKind", draft.contentKind());
        if (draft.layoutConfidence() != null) {
            metadata.put("layoutConfidence", draft.layoutConfidence());
        }
        if (draft.ocrConfidence() != null) {
            metadata.put("ocrConfidence", draft.ocrConfidence());
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize chunk metadata", exception);
        }
    }

    private List<KbDocChunk> toChunks(
            KbDocument document,
            DocumentParser parser,
            List<ChunkDraft> drafts,
            EmbeddingBatchResult embeddingResult,
            VectorUpsertResult vectorResult
    ) {
        List<KbDocChunk> chunks = new ArrayList<>();
        Set<String> seenHashes = new java.util.LinkedHashSet<>();
        for (ChunkDraft draft : drafts) {
            String chunkHash = chunkHash(document, draft);
            if (!seenHashes.add(chunkHash)) {
                continue;
            }
            KbDocChunk chunk = new KbDocChunk();
            chunk.setId("chk_" + UUID.randomUUID().toString().replace("-", ""));
            chunk.setKbId(document.getKbId());
            chunk.setDocumentId(document.getId());
            chunk.setDocumentVersion(document.getVersion());
            chunk.setChunkIndex(chunks.size());
            chunk.setContent(draft.content());
            chunk.setPageNum(draft.pageNum());
            chunk.setSectionTitle(draft.sectionTitle());
            chunk.setChunkHash(chunkHash);
            chunk.setMetadataJson(metadataJson(parser, draft, embeddingResult, vectorResult));
            chunks.add(chunk);
        }
        return chunks;
    }

    private EmbeddingBatchResult embedDocumentChunks(KbDocument document, List<KbDocChunk> chunks) {
        List<ChunkEmbeddingInput> inputs = chunks.stream()
                .map(chunk -> new ChunkEmbeddingInput(chunk.getId(), chunk.getContent()))
                .toList();
        return embeddingService.embedDocumentChunks(document.getId(), document.getKbId(), inputs);
    }

    private VectorUpsertResult deleteVectorDocument(KbDocument document) {
        return vectorIndexAdapter.deleteDocument(
                document.getKbId(),
                document.getId(),
                document.getVersion()
        );
    }

    private VectorUpsertResult upsertVectorIndex(
            KbDocument document,
            List<KbDocChunk> chunks,
            EmbeddingBatchResult embeddingResult
    ) {
        Map<String, EmbeddingVector> vectorsByChunkId = embeddingVectors(embeddingResult);
        List<VectorChunkReference> references = chunks.stream()
                .map(chunk -> new VectorChunkReference(
                        chunk.getId(),
                        chunk.getChunkHash(),
                        chunk.getChunkIndex(),
                        vectorsByChunkId.get(chunk.getId())
                ))
                .toList();
        return vectorIndexAdapter.upsert(new VectorUpsertRequest(
                document.getKbId(),
                document.getId(),
                document.getVersion(),
                references
        ));
    }

    private Map<String, EmbeddingVector> embeddingVectors(EmbeddingBatchResult embeddingResult) {
        if (embeddingResult == null || embeddingResult.vectors().isEmpty()) {
            return Map.of();
        }
        return embeddingResult.vectors().stream()
                .filter(vector -> vector != null && vector.ownerId() != null)
                .collect(java.util.stream.Collectors.toMap(
                        EmbeddingVector::ownerId,
                        java.util.function.Function.identity(),
                        (left, right) -> left
                ));
    }

    private void ensureEmbeddingSucceeded(EmbeddingBatchResult embeddingResult) {
        if (embeddingResult.status() == EmbeddingStatus.DISABLED
                || embeddingResult.status() == EmbeddingStatus.SUCCEEDED) {
            return;
        }
        throw new IndexingException(safePipelineError(embeddingResult.errorCode(), "EMBEDDING_FAILED"));
    }

    private void ensureVectorUpsertSucceeded(VectorUpsertResult vectorResult) {
        if (vectorResult.status() == VectorIndexStatus.DISABLED
                || vectorResult.status() == VectorIndexStatus.SUCCEEDED) {
            return;
        }
        throw new IndexingException(safePipelineError(vectorResult.errorCode(), "VECTOR_UPSERT_FAILED"));
    }

    private String safePipelineError(String errorCode, String fallback) {
        if (errorCode == null || errorCode.isBlank()) {
            return fallback;
        }
        return errorCode.trim();
    }

    private void applyIndexPipelineMetadata(
            List<KbDocChunk> chunks,
            EmbeddingBatchResult embeddingResult,
            VectorUpsertResult vectorResult
    ) {
        for (KbDocChunk chunk : chunks) {
            chunk.setMetadataJson(mergePipelineMetadata(chunk.getMetadataJson(), embeddingResult, vectorResult));
        }
    }

    private String mergePipelineMetadata(
            String existingMetadataJson,
            EmbeddingBatchResult embeddingResult,
            VectorUpsertResult vectorResult
    ) {
        Map<String, Object> metadata = readMetadata(existingMetadataJson);
        metadata.put("embeddingModel", embeddingResult.modelVersion());
        metadata.put("embeddingStatus", embeddingResult.status().name());
        metadata.put("vectorIndexStatus", vectorResult.status().name());
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize chunk metadata", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(metadataJson, LinkedHashMap.class);
        } catch (JsonProcessingException exception) {
            return new LinkedHashMap<>();
        }
    }

    private List<String> tokens(String content) {
        return Arrays.stream(content.split("\\s+"))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .toList();
    }

    private String joinTokens(List<String> tokens, int start, int end) {
        return String.join(" ", tokens.subList(start, end));
    }

    private String chunkHash(KbDocument document, ChunkDraft draft) {
        String input = String.join("\n",
                nullToEmpty(document.getId()),
                String.valueOf(document.getVersion()),
                String.join("\n", draft.headingPath()),
                draft.content()
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private void markClaimed(KbIndexTask task, String workerId, Instant now, Duration leaseDuration) {
        task.setStatus(IndexTaskStatus.RUNNING);
        task.setProgressPhase("CLAIMED");
        task.setProgressPercent(5);
        task.setHeartbeatAt(now);
        task.setLeaseOwner(workerId);
        task.setLeaseUntil(now.plus(leaseDuration));
        task.setRecoverable(true);
        task.setNextRetryAt(null);
        task.setErrorMessage(null);
        if (task.getStartedAt() == null) {
            task.setStartedAt(now);
        }
    }

    private void markRunningStage(
            KbIndexTask task,
            String workerId,
            Instant now,
            Duration leaseDuration,
            String phase,
            int progressPercent
    ) {
        applyRunningStage(task, workerId, now, leaseDuration, phase, progressPercent);
        stageTransactionTemplate.executeWithoutResult(status -> indexTaskRepository.findById(task.getId())
                .filter(persisted -> persisted.getStatus() != IndexTaskStatus.SUCCEEDED)
                .filter(persisted -> persisted.getStatus() != IndexTaskStatus.FAILED)
                .ifPresent(persisted -> {
                    applyRunningStage(persisted, workerId, now, leaseDuration, phase, progressPercent);
                    indexTaskRepository.saveAndFlush(persisted);
                }));
    }

    private void applyRunningStage(
            KbIndexTask task,
            String workerId,
            Instant now,
            Duration leaseDuration,
            String phase,
            int progressPercent
    ) {
        task.setStatus(IndexTaskStatus.RUNNING);
        task.setProgressPhase(phase);
        task.setProgressPercent(progressPercent);
        task.setHeartbeatAt(now);
        if (workerId != null && !workerId.isBlank()) {
            task.setLeaseOwner(workerId.trim());
        }
        task.setLeaseUntil(now.plus(leaseDuration));
        task.setNextRetryAt(null);
        task.setRecoverable(true);
        task.setErrorMessage(null);
        if (task.getStartedAt() == null) {
            task.setStartedAt(now);
        }
    }

    private void markIndexFailed(
            KbIndexTask task,
            KbDocument document,
            Exception exception,
            int maxRetryCount,
            Duration retryBackoff
    ) {
        String message = safeError(exception);
        markRetryOrTerminalFailure(task, message, maxRetryCount, retryBackoff);
        if (document != null) {
            document.setParseStatus(DocumentStatus.FAILED);
            document.setIndexStatus(DocumentStatus.FAILED);
            document.setErrorMessage(message);
        }
    }

    private String safeError(Exception exception) {
        if (exception instanceof IndexingException indexingException) {
            return indexingException.safeCode();
        }
        if (exception instanceof DocumentParseException parseException) {
            return parseException.safeCode();
        }
        if (exception instanceof IOException) {
            return "DOCUMENT_READ_FAILED";
        }
        if (exception instanceof IllegalStateException) {
            return "DOCUMENT_INDEX_FAILED";
        }
        return "DOCUMENT_INDEX_UNEXPECTED_ERROR";
    }

    private void markRetryOrTerminalFailure(
            KbIndexTask task,
            String safeError,
            int maxRetryCount,
            Duration retryBackoff
    ) {
        Instant now = Instant.now();
        int nextRetryCount = task.getRetryCount() + 1;
        task.setRetryCount(nextRetryCount);
        task.setErrorMessage(safeError);
        task.setHeartbeatAt(now);
        task.setLeaseOwner(null);
        task.setLeaseUntil(null);
        if (nextRetryCount <= maxRetryCount) {
            Duration safeBackoff = retryBackoff == null || retryBackoff.isNegative() || retryBackoff.isZero()
                    ? Duration.ofSeconds(30)
                    : retryBackoff;
            task.setStatus(IndexTaskStatus.PENDING);
            task.setProgressPhase("RETRY_WAIT");
            task.setProgressPercent(Math.max(0, Math.min(task.getProgressPercent(), 99)));
            task.setNextRetryAt(now.plus(safeBackoff));
            task.setRecoverable(true);
            task.setFinishedAt(null);
            return;
        }
        task.setStatus(IndexTaskStatus.FAILED);
        task.setProgressPhase("FAILED");
        task.setNextRetryAt(null);
        task.setRecoverable(false);
        task.setFinishedAt(now);
    }

    private String clean(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace('\u0000', ' ')
                .replaceAll("\\p{Cntrl}", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record ChunkDraft(
            String content,
            String sectionTitle,
            Integer headingLevel,
            List<String> headingPath,
            Integer pageNum,
            String pageNumSource,
            Integer readingOrderIndex,
            Double layoutConfidence,
            Double ocrConfidence,
            String contentKind,
            int tokenCount
    ) {
    }

    private static final class IndexingException extends RuntimeException {
        private final String safeCode;

        private IndexingException(String safeCode) {
            super(safeCode);
            this.safeCode = safeCode;
        }

        private String safeCode() {
            return safeCode;
        }
    }
}
