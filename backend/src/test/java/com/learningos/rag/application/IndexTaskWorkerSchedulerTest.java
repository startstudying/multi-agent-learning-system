package com.learningos.rag.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningos.config.IndexWorkerProperties;
import com.learningos.config.StorageProperties;
import com.learningos.rag.domain.KbDocChunk;
import com.learningos.rag.domain.KbDocument;
import com.learningos.rag.domain.KbIndexTask;
import com.learningos.rag.domain.KnowledgeBase;
import com.learningos.rag.domain.enums.DocumentStatus;
import com.learningos.rag.domain.enums.IndexTaskStatus;
import com.learningos.rag.domain.enums.Visibility;
import com.learningos.rag.parser.DocumentParserService;
import com.learningos.rag.repository.KbDocChunkRepository;
import com.learningos.rag.repository.KbDocumentRepository;
import com.learningos.rag.repository.KbIndexTaskRepository;
import com.learningos.rag.repository.KnowledgeBaseRepository;
import com.learningos.rag.storage.DocumentStorageService;
import com.learningos.rag.storage.StoredObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({
        IndexService.class,
        DocumentParserService.class,
        EmbeddingService.class,
        NoopVectorIndexAdapter.class,
        IndexTaskWorkerSchedulerTest.IndexTaskWorkerSchedulerTestConfig.class
})
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class IndexTaskWorkerSchedulerTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T08:00:00Z"),
            ZoneOffset.UTC
    );

    private final IndexService indexService;
    private final KbIndexTaskRepository indexTaskRepository;
    private final KbDocumentRepository documentRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KbDocChunkRepository chunkRepository;
    private final TestDocumentStorageService storageService;

    IndexTaskWorkerSchedulerTest(
            IndexService indexService,
            KbIndexTaskRepository indexTaskRepository,
            KbDocumentRepository documentRepository,
            KnowledgeBaseRepository knowledgeBaseRepository,
            KbDocChunkRepository chunkRepository,
            TestDocumentStorageService storageService
    ) {
        this.indexService = indexService;
        this.indexTaskRepository = indexTaskRepository;
        this.documentRepository = documentRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.chunkRepository = chunkRepository;
        this.storageService = storageService;
    }

    @Test
    void workerProcessesDuePendingTaskAndMarksDocumentIndexed() {
        KbDocument document = saveDocument("worker.md");
        storageService.put(document, "Worker should parse and index this document.");
        KbIndexTask task = indexService.createPendingTask(document);
        IndexTaskWorkerScheduler worker = worker(true);

        int processed = worker.runOnce();

        assertThat(processed).isEqualTo(1);
        assertThat(indexTaskRepository.findById(task.getId()))
                .hasValueSatisfying(processedTask -> {
                    assertThat(processedTask.getStatus()).isEqualTo(IndexTaskStatus.SUCCEEDED);
                    assertThat(processedTask.getProgressPercent()).isEqualTo(100);
                    assertThat(processedTask.getProgressPhase()).isEqualTo("SUCCEEDED");
                    assertThat(processedTask.getHeartbeatAt()).isNotNull();
                    assertThat(processedTask.getLeaseOwner()).isNull();
                    assertThat(processedTask.getLeaseUntil()).isNull();
                    assertThat(processedTask.isRecoverable()).isFalse();
                });
        assertThat(documentRepository.findById(document.getId()))
                .hasValueSatisfying(indexedDocument -> assertThat(indexedDocument.getIndexStatus()).isEqualTo(DocumentStatus.INDEXED));
        assertThat(chunkRepository.findAll().stream()
                .filter(chunk -> document.getId().equals(chunk.getDocumentId()))
                .count()).isEqualTo(1);
    }

    @Test
    void workerProducesProductionChunkMetadata() {
        KbDocument document = saveDocument("worker-production.md");
        storageService.put(document, """
                # Worker heading

                %s
                """.formatted(numberedTokens("worker", 240)));
        indexService.createPendingTask(document);
        IndexTaskWorkerScheduler worker = worker(true);

        int processed = worker.runOnce();

        assertThat(processed).isEqualTo(1);
        List<KbDocChunk> chunks = chunkRepository.findAll().stream()
                .filter(chunk -> document.getId().equals(chunk.getDocumentId()))
                .toList();
        assertThat(chunks).hasSizeGreaterThanOrEqualTo(2);
        assertThat(chunks.getFirst().getMetadataJson())
                .contains("\"chunkingStrategy\":\"TOKEN_WINDOW_V1\"")
                .contains("\"overlapTokenCount\":40")
                .contains("\"headingPath\":[\"Worker heading\"]");
        assertThat(chunkHash(chunks.getFirst())).matches("[a-f0-9]{64}");
    }

    @Test
    void disabledWorkerDoesNotProcessPendingTasks() {
        KbDocument document = saveDocument("disabled.md");
        storageService.put(document, "Disabled worker should not process this document.");
        KbIndexTask task = indexService.createPendingTask(document);
        IndexTaskWorkerScheduler worker = worker(false);

        int processed = worker.runOnce();

        assertThat(processed).isZero();
        assertThat(indexTaskRepository.findById(task.getId()))
                .hasValueSatisfying(pendingTask -> assertThat(pendingTask.getStatus()).isEqualTo(IndexTaskStatus.PENDING));
    }

    @Test
    void workerContinuesBatchWhenOneClaimedTaskFailsBeforeDocumentLoad() {
        KbDocument missingDocument = saveDocument("missing.md");
        KbIndexTask missingTask = indexService.createPendingTask(missingDocument);
        documentRepository.delete(missingDocument);
        documentRepository.flush();
        KbDocument okDocument = saveDocument("ok.md");
        storageService.put(okDocument, "Worker should continue after one claimed task fails.");
        KbIndexTask okTask = indexService.createPendingTask(okDocument);
        IndexTaskWorkerScheduler worker = worker(true);

        int claimed = worker.runOnce();

        assertThat(claimed).isEqualTo(2);
        assertThat(indexTaskRepository.findById(missingTask.getId()))
                .hasValueSatisfying(task -> {
                    assertThat(task.getStatus()).isEqualTo(IndexTaskStatus.PENDING);
                    assertThat(task.getProgressPhase()).isEqualTo("RETRY_WAIT");
                    assertThat(task.getErrorMessage()).isEqualTo("DOCUMENT_EMPTY_OR_UNAVAILABLE");
                    assertThat(task.isRecoverable()).isTrue();
                });
        assertThat(indexTaskRepository.findById(okTask.getId()))
                .hasValueSatisfying(task -> assertThat(task.getStatus()).isEqualTo(IndexTaskStatus.SUCCEEDED));
    }

    private IndexTaskWorkerScheduler worker(boolean enabled) {
        IndexWorkerProperties properties = new IndexWorkerProperties(
                enabled,
                Duration.ofSeconds(5),
                2,
                Duration.ofMinutes(2),
                Duration.ofSeconds(30),
                2
        );
        return new IndexTaskWorkerScheduler(indexService, properties, FIXED_CLOCK, "test-worker");
    }

    private KbDocument saveDocument(String name) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setName("SQL course");
        knowledgeBase.setDescription("Database lessons");
        knowledgeBase.setVisibility(Visibility.PRIVATE);
        knowledgeBase.setOwnerUserId("alice");
        knowledgeBase.setCreatedBy("alice");
        knowledgeBase = knowledgeBaseRepository.saveAndFlush(knowledgeBase);

        KbDocument document = new KbDocument();
        document.setKbId(knowledgeBase.getId());
        document.setKnowledgeBase(knowledgeBase);
        document.setName(name);
        document.setContentType("text/markdown");
        document.setSizeBytes(64L);
        document.setStorageBucket("learning-os-documents");
        document.setStorageKey(knowledgeBase.getId() + "/" + name);
        document.setCreatedBy("alice");
        return documentRepository.saveAndFlush(document);
    }

    private String chunkHash(KbDocChunk chunk) {
        try {
            Method method = KbDocChunk.class.getMethod("getChunkHash");
            Object value = method.invoke(chunk);
            return value == null ? null : value.toString();
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("KbDocChunk must expose stable chunkHash", exception);
        }
    }

    private String numberedTokens(String prefix, int count) {
        List<String> values = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            values.add(prefix + String.format("%03d", i));
        }
        return String.join(" ", values);
    }

    @TestConfiguration
    static class IndexTaskWorkerSchedulerTestConfig {

        @Bean
        TestDocumentStorageService documentStorageService() {
            return new TestDocumentStorageService();
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    static class TestDocumentStorageService implements DocumentStorageService {

        private final Map<String, byte[]> objects = new HashMap<>();

        @Override
        public StoredObject store(String kbId, MultipartFile file) throws IOException {
            String key = kbId + "/" + file.getOriginalFilename();
            objects.put(key, file.getBytes());
            return new StoredObject("learning-os-documents", key, file.getSize(), file.getContentType());
        }

        @Override
        public byte[] read(String bucket, String key) {
            return objects.get(bucket + "/" + key);
        }

        void put(KbDocument document, String content) {
            objects.put(
                    document.getStorageBucket() + "/" + document.getStorageKey(),
                    content.getBytes(StandardCharsets.UTF_8)
            );
        }
    }
}
