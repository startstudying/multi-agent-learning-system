package com.learningos.rag.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.learningos.config.StorageProperties;
import com.learningos.rag.domain.KbDocChunk;
import com.learningos.rag.domain.KbIndexTask;
import com.learningos.rag.domain.KbDocument;
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
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({
        IndexService.class,
        DocumentParserService.class,
        EmbeddingService.class,
        NoopVectorIndexAdapter.class,
        IndexServiceTest.IndexServiceTestConfig.class
})
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class IndexServiceTest {

    private final IndexService indexService;
    private final KbIndexTaskRepository indexTaskRepository;
    private final KbDocumentRepository documentRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KbDocChunkRepository chunkRepository;
    private final TestDocumentStorageService storageService;
    private final ObjectMapper objectMapper;

    IndexServiceTest(
            IndexService indexService,
            KbIndexTaskRepository indexTaskRepository,
            KbDocumentRepository documentRepository,
            KnowledgeBaseRepository knowledgeBaseRepository,
            KbDocChunkRepository chunkRepository,
            TestDocumentStorageService storageService,
            ObjectMapper objectMapper
    ) {
        this.indexService = indexService;
        this.indexTaskRepository = indexTaskRepository;
        this.documentRepository = documentRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.chunkRepository = chunkRepository;
        this.storageService = storageService;
        this.objectMapper = objectMapper;
    }

    @Test
    void documentRepositoryExposesPessimisticWriteLockForIndexTaskCreation() throws Exception {
        var method = KbDocumentRepository.class.getMethod("findByIdAndDeletedAtIsNullForUpdate", String.class);

        Lock lock = method.getAnnotation(Lock.class);

        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void createPendingTaskReusesSingleActiveTaskWhenConcurrentReindexStartsTogether() throws Exception {
        KbDocument document = saveDocument("concurrent-lock.md");
        int workers = 2;
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < workers; i++) {
            futures.add(executor.submit(() -> {
                KbDocument loaded = documentRepository.findById(document.getId()).orElseThrow();
                ready.countDown();
                assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                return indexService.createPendingTask(loaded).getId();
            }));
        }

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        List<String> taskIds = new ArrayList<>();
        for (Future<String> future : futures) {
            taskIds.add(future.get(10, TimeUnit.SECONDS));
        }
        executor.shutdownNow();

        assertThat(taskIds).hasSize(workers);
        assertThat(taskIds).containsOnly(taskIds.getFirst());
        assertThat(indexTaskRepository.findAll().stream()
                .filter(task -> document.getId().equals(task.getDocumentId()))
                .filter(task -> task.getStatus() == IndexTaskStatus.PENDING || task.getStatus() == IndexTaskStatus.RUNNING)
                .count()).isEqualTo(1);
    }

    @Test
    void recoversTimedOutRunningTasksAndKeepsNonExpiredOrTerminalTasksUnchanged() {
        Instant cutoff = Instant.now().plusSeconds(3600);
        KbIndexTask timedOut = saveTask("doc_timeout", IndexTaskStatus.RUNNING, cutoff.minusSeconds(600));
        timedOut.setRetryCount(2);
        timedOut.setLeaseUntil(cutoff.minusSeconds(1));
        indexTaskRepository.saveAndFlush(timedOut);
        KbIndexTask freshRunning = saveTask("doc_fresh", IndexTaskStatus.RUNNING, cutoff.plusSeconds(60));
        freshRunning.setLeaseUntil(cutoff.plusSeconds(600));
        indexTaskRepository.saveAndFlush(freshRunning);
        KbIndexTask pending = saveTask("doc_pending", IndexTaskStatus.PENDING, cutoff.minusSeconds(600));
        KbIndexTask succeeded = saveTask("doc_succeeded", IndexTaskStatus.SUCCEEDED, cutoff.minusSeconds(600));
        KbIndexTask failed = saveTask("doc_failed", IndexTaskStatus.FAILED, cutoff.minusSeconds(600));

        int recovered = indexService.recoverTimedOutRunningTasks(cutoff, 3, Duration.ofSeconds(30));

        assertThat(recovered).isEqualTo(1);
        assertThat(indexTaskRepository.findById(timedOut.getId()))
                .hasValueSatisfying(task -> {
                    assertThat(task.getStatus()).isEqualTo(IndexTaskStatus.PENDING);
                    assertThat(task.getRetryCount()).isEqualTo(3);
                    assertThat(task.getErrorMessage()).isEqualTo("DOCUMENT_INDEX_LEASE_EXPIRED");
                    assertThat(task.getProgressPhase()).isEqualTo("RETRY_WAIT");
                    assertThat(task.getNextRetryAt()).isNotNull();
                    assertThat(task.getFinishedAt()).isNull();
                });
        assertThat(indexTaskRepository.findById(freshRunning.getId()))
                .hasValueSatisfying(task -> {
                    assertThat(task.getStatus()).isEqualTo(IndexTaskStatus.RUNNING);
                    assertThat(task.getRetryCount()).isZero();
                    assertThat(task.getErrorMessage()).isNull();
                    assertThat(task.getFinishedAt()).isNull();
                });
        assertThat(indexTaskRepository.findById(pending.getId()))
                .hasValueSatisfying(task -> assertThat(task.getStatus()).isEqualTo(IndexTaskStatus.PENDING));
        assertThat(indexTaskRepository.findById(succeeded.getId()))
                .hasValueSatisfying(task -> assertThat(task.getStatus()).isEqualTo(IndexTaskStatus.SUCCEEDED));
        assertThat(indexTaskRepository.findById(failed.getId()))
                .hasValueSatisfying(task -> assertThat(task.getStatus()).isEqualTo(IndexTaskStatus.FAILED));
    }

    @Test
    void processMarkdownIndexTaskCleansChunksAndMarksDocumentIndexed() {
        KbDocument document = saveDocument("joins.md", "text/markdown");
        storageService.put(document, """
                # Join basics

                JOIN duplicates usually come from one-to-many relationships. %s

                ## Index strategy

                Indexes help lookup speed but do not remove duplicate rows. %s
                """.formatted("Cardinality matters. ".repeat(60), "Use EXPLAIN. ".repeat(60)));
        KbIndexTask task = indexService.createPendingTask(document);

        KbIndexTask processed = indexService.processIndexTask(task.getId());

        assertThat(processed.getStatus()).isEqualTo(IndexTaskStatus.SUCCEEDED);
        assertThat(processed.getProgressPercent()).isEqualTo(100);
        assertThat(processed.getProgressPhase()).isEqualTo("SUCCEEDED");
        assertThat(processed.getHeartbeatAt()).isNotNull();
        assertThat(processed.getLeaseUntil()).isNull();
        assertThat(processed.isRecoverable()).isFalse();
        assertThat(processed.getStartedAt()).isNotNull();
        assertThat(processed.getFinishedAt()).isNotNull();
        assertThat(processed.getErrorMessage()).isNull();
        assertThat(documentRepository.findById(document.getId()))
                .hasValueSatisfying(indexedDocument -> {
                    assertThat(indexedDocument.getParseStatus()).isEqualTo(DocumentStatus.INDEXED);
                    assertThat(indexedDocument.getIndexStatus()).isEqualTo(DocumentStatus.INDEXED);
                    assertThat(indexedDocument.getErrorMessage()).isNull();
                });
        List<KbDocChunk> chunks = chunksFor(document.getId());
        assertThat(chunks).hasSizeGreaterThanOrEqualTo(2);
        assertThat(chunks)
                .allSatisfy(chunk -> {
                    assertThat(chunk.getContent()).doesNotContain("  ");
                    assertThat(chunk.getContent().length()).isLessThanOrEqualTo(1000);
                    assertThat(chunk.getMetadataJson())
                            .contains("\"parser\":\"MARKDOWN\"")
                            .contains("\"embeddingModel\":\"noop-embedding-v1\"")
                            .contains("\"embeddingStatus\":\"DISABLED\"")
                            .contains("\"vectorIndexStatus\":\"DISABLED\"");
                });
        assertThat(chunks.getFirst().getSectionTitle()).isEqualTo("Join basics");
        assertThat(chunks.stream().map(KbDocChunk::getSectionTitle)).contains("Index strategy");
    }

    @Test
    void processMarkdownIndexTaskUsesTokenWindowOverlapStableHashAndHeadingHierarchy() throws Exception {
        KbDocument document = saveDocument("token-window.md", "text/markdown");
        storageService.put(document, """
                # SQL Foundations

                Short introduction for the course.

                ## Advanced joins

                %s
                """.formatted(numberedTokens("join", 280)));
        KbIndexTask task = indexService.createPendingTask(document);

        indexService.processIndexTask(task.getId());

        List<KbDocChunk> joinChunks = chunksFor(document.getId()).stream()
                .filter(chunk -> "Advanced joins".equals(chunk.getSectionTitle()))
                .toList();
        assertThat(joinChunks).hasSizeGreaterThanOrEqualTo(2);

        List<String> firstTokens = tokens(joinChunks.get(0).getContent());
        List<String> secondTokens = tokens(joinChunks.get(1).getContent());
        assertThat(firstTokens).hasSizeLessThanOrEqualTo(220);
        assertThat(secondTokens.subList(0, 40))
                .containsExactlyElementsOf(firstTokens.subList(firstTokens.size() - 40, firstTokens.size()));

        Map<String, Object> metadata = metadata(joinChunks.getFirst());
        assertThat(metadata)
                .containsEntry("parser", "MARKDOWN")
                .containsEntry("embeddingModel", "noop-embedding-v1")
                .containsEntry("embeddingStatus", "DISABLED")
                .containsEntry("vectorIndexStatus", "DISABLED")
                .containsEntry("chunkingStrategy", "TOKEN_WINDOW_V1")
                .containsEntry("headingLevel", 2)
                .containsEntry("pageNumSource", "NONE")
                .containsEntry("readingOrderIndex", 2)
                .containsEntry("contentKind", "TEXT")
                .containsEntry("overlapTokenCount", 40);
        assertThat(metadata).doesNotContainKeys("layoutConfidence", "ocrConfidence", "rawOcrText", "providerResponse");
        assertThat(metadata.get("headingPath"))
                .isEqualTo(List.of("SQL Foundations", "Advanced joins"));
        assertThat(chunkHash(joinChunks.getFirst())).matches("[a-f0-9]{64}");
    }

    @Test
    void reindexKeepsStableChunkHashesForSameDocumentVersion() {
        KbDocument document = saveDocument("stable-hash.md", "text/markdown");
        storageService.put(document, """
                # Stable indexing

                %s
                """.formatted(numberedTokens("stable", 260)));

        indexService.processIndexTask(indexService.createPendingTask(document).getId());
        List<KbDocChunk> firstChunks = chunksFor(document.getId());
        List<String> firstContents = firstChunks.stream().map(KbDocChunk::getContent).toList();
        List<String> firstHashes = firstChunks.stream().map(this::chunkHash).toList();

        KbDocument reloaded = documentRepository.findById(document.getId()).orElseThrow();
        indexService.processIndexTask(indexService.createPendingTask(reloaded).getId());

        List<KbDocChunk> secondChunks = chunksFor(document.getId());
        assertThat(secondChunks.stream().map(KbDocChunk::getContent).toList())
                .containsExactlyElementsOf(firstContents);
        assertThat(secondChunks.stream().map(this::chunkHash).toList())
                .containsExactlyElementsOf(firstHashes);
    }

    @Test
    void sameTextUnderDifferentHeadingsIsNotDeduplicatedAway() {
        KbDocument document = saveDocument("heading-duplicates.md", "text/markdown");
        String repeated = "Repeatable SQL definition belongs to each section.";
        storageService.put(document, """
                # First heading

                %s

                # Second heading

                %s
                """.formatted(repeated, repeated));

        indexService.processIndexTask(indexService.createPendingTask(document).getId());

        List<KbDocChunk> chunks = chunksFor(document.getId());
        assertThat(chunks).hasSize(2);
        assertThat(chunks).extracting(KbDocChunk::getSectionTitle)
                .containsExactly("First heading", "Second heading");
        assertThat(chunks).extracting(this::chunkHash).doesNotHaveDuplicates();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void processIndexTaskCommitsRunningProgressBeforeStorageReadCompletes() throws Exception {
        KbDocument document = saveDocument("slow.md", "text/markdown");
        storageService.put(document, "# Slow read\n\nProgress should be visible while storage read is blocked.");
        KbIndexTask task = indexService.createPendingTask(document);
        storageService.blockReads();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        Future<KbIndexTask> future = executor.submit(() -> indexService.processIndexTask(
                task.getId(),
                "worker-a",
                2,
                Duration.ofSeconds(30),
                Duration.ofMinutes(2)
        ));

        assertThat(storageService.awaitReadStarted()).isTrue();
        assertThat(indexTaskRepository.findById(task.getId()))
                .hasValueSatisfying(running -> {
                    assertThat(running.getStatus()).isEqualTo(IndexTaskStatus.RUNNING);
                    assertThat(running.getProgressPhase()).isEqualTo("PARSING");
                    assertThat(running.getProgressPercent()).isEqualTo(20);
                    assertThat(running.getHeartbeatAt()).isNotNull();
                    assertThat(running.getLeaseOwner()).isEqualTo("worker-a");
                    assertThat(running.getLeaseUntil()).isNotNull();
                });

        storageService.releaseReads();
        assertThat(future.get(10, TimeUnit.SECONDS).getStatus()).isEqualTo(IndexTaskStatus.SUCCEEDED);
        executor.shutdownNow();
    }

    @Test
    void processTxtPdfAndDocxIndexTasksProduceSearchableChunks() throws Exception {
        KbDocument txt = saveDocument("outline.txt", "text/plain");
        storageService.put(txt, "Retrieval augmented generation needs chunks with citations.");
        KbDocument pdf = saveDocument("retrieval.pdf", "application/pdf");
        storageService.put(pdf, "BT (Hybrid retrieval combines keyword search and vectors.) Tj ET");
        KbDocument docx = saveDocument("rubric.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        storageService.put(docx, docxBytes("Rubric grading should compare criteria and wrong causes."));

        indexService.processIndexTask(indexService.createPendingTask(txt).getId());
        indexService.processIndexTask(indexService.createPendingTask(pdf).getId());
        indexService.processIndexTask(indexService.createPendingTask(docx).getId());

        assertThat(chunksFor(txt.getId()).getFirst().getContent()).contains("Retrieval augmented generation");
        assertThat(chunksFor(pdf.getId()).getFirst().getContent()).contains("Hybrid retrieval combines");
        assertThat(chunksFor(docx.getId()).getFirst().getContent()).contains("Rubric grading");
        assertThat(chunksFor(txt.getId()).getFirst().getMetadataJson()).contains("\"parser\":\"TXT\"");
        assertThat(chunksFor(pdf.getId()).getFirst().getMetadataJson()).contains("\"parser\":\"PDF\"");
        assertThat(chunksFor(docx.getId()).getFirst().getMetadataJson()).contains("\"parser\":\"DOCX\"");
    }

    @Test
    void processScannedPdfDoesNotPersistRawBinaryChunks() {
        KbDocument pdf = saveDocument("scan.pdf", "application/pdf");
        storageService.put(pdf, """
                %PDF-1.7
                1 0 obj <</Length 40>> stream
                x\u0000\u0001binary object stream should not be indexed
                endstream endobj
                %%EOF
                """.getBytes(StandardCharsets.ISO_8859_1));
        KbIndexTask task = indexService.createPendingTask(pdf);

        KbIndexTask processed = indexService.processIndexTask(task.getId());

        assertThat(processed.getStatus()).isEqualTo(IndexTaskStatus.FAILED);
        assertThat(processed.getErrorMessage()).isEqualTo("DOCUMENT_EMPTY_OR_UNAVAILABLE");
        assertThat(chunksFor(pdf.getId())).isEmpty();
        assertThat(documentRepository.findById(pdf.getId()))
                .hasValueSatisfying(saved -> {
                    assertThat(saved.getParseStatus()).isEqualTo(DocumentStatus.FAILED);
                    assertThat(saved.getIndexStatus()).isEqualTo(DocumentStatus.FAILED);
                    assertThat(saved.getErrorMessage()).isEqualTo("DOCUMENT_EMPTY_OR_UNAVAILABLE");
                });
    }

    @Test
    void processSimpleMultiPagePdfPreservesPageNumbersInChunks() {
        KbDocument pdf = saveDocument("multi-page.pdf", "application/pdf");
        storageService.put(pdf, """
                1 0 obj <</Type /Page>> endobj
                BT (First page retrieval note.) Tj ET
                2 0 obj <</Type /Page>> endobj
                BT (Second page citation detail.) Tj ET
                """.getBytes(StandardCharsets.ISO_8859_1));

        KbIndexTask processed = indexService.processIndexTask(indexService.createPendingTask(pdf).getId());

        assertThat(processed.getStatus()).isEqualTo(IndexTaskStatus.SUCCEEDED);
        List<KbDocChunk> chunks = chunksFor(pdf.getId());
        assertThat(chunks).hasSize(2);
        assertThat(chunks)
                .extracting(KbDocChunk::getContent, KbDocChunk::getPageNum)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("First page retrieval note.", 1),
                        org.assertj.core.groups.Tuple.tuple("Second page citation detail.", 2)
                );
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.getMetadataJson()).contains("\"parser\":\"PDF\""));
    }

    @Test
    void processDocxPreservesHeadingPathAndPageNumberMetadata() throws Exception {
        KbDocument docx = saveDocument(
                "lesson.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );
        storageService.put(docx, docxXmlBytes("""
                <w:p><w:pPr><w:pStyle w:val="Heading1"/></w:pPr><w:r><w:t>Course RAG</w:t></w:r></w:p>
                <w:p><w:r><w:t>Course overview.</w:t></w:r></w:p>
                <w:p><w:pPr><w:pStyle w:val="Heading2"/></w:pPr><w:r><w:t>Citations</w:t></w:r></w:p>
                <w:p><w:r><w:br w:type="page"/><w:t>Second page citation detail.</w:t></w:r></w:p>
                """));

        KbIndexTask processed = indexService.processIndexTask(indexService.createPendingTask(docx).getId());

        assertThat(processed.getStatus()).isEqualTo(IndexTaskStatus.SUCCEEDED);
        List<KbDocChunk> chunks = chunksFor(docx.getId());
        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).getSectionTitle()).isEqualTo("Course RAG");
        assertThat(chunks.get(0).getPageNum()).isEqualTo(1);
        Map<String, Object> firstMetadata = metadata(chunks.get(0));
        assertThat(firstMetadata.get("headingPath")).isEqualTo(List.of("Course RAG"));
        assertThat(firstMetadata)
                .containsEntry("pageNumSource", "PARSER_INFERRED")
                .containsEntry("readingOrderIndex", 1)
                .containsEntry("contentKind", "TEXT");
        assertThat(chunks.get(1).getSectionTitle()).isEqualTo("Citations");
        assertThat(chunks.get(1).getPageNum()).isEqualTo(2);
        Map<String, Object> secondMetadata = metadata(chunks.get(1));
        assertThat(secondMetadata.get("headingPath")).isEqualTo(List.of("Course RAG", "Citations"));
        assertThat(secondMetadata)
                .containsEntry("pageNumSource", "PARSER_INFERRED")
                .containsEntry("readingOrderIndex", 2)
                .containsEntry("contentKind", "TEXT");
    }

    @Test
    void processDocxTablePreservesReadingOrderAndTableContentKindMetadata() throws Exception {
        KbDocument docx = saveDocument(
                "table.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );
        storageService.put(docx, docxXmlBytes("""
                <w:p><w:pPr><w:pStyle w:val="Heading1"/></w:pPr><w:r><w:t>Course RAG</w:t></w:r></w:p>
                <w:p><w:pPr><w:pStyle w:val="TOC1"/></w:pPr><w:r><w:t>Course RAG .... 1</w:t></w:r></w:p>
                <w:p><w:r><w:t>Intro paragraph.</w:t></w:r></w:p>
                <w:tbl>
                  <w:tr>
                    <w:tc><w:p><w:r><w:t>Concept</w:t></w:r></w:p></w:tc>
                    <w:tc><w:p><w:r><w:t>Definition</w:t></w:r></w:p></w:tc>
                  </w:tr>
                  <w:tr>
                    <w:tc><w:p><w:r><w:t>RAG</w:t></w:r></w:p></w:tc>
                    <w:tc><w:p><w:r><w:t>Retrieval augmented generation</w:t></w:r></w:p></w:tc>
                  </w:tr>
                </w:tbl>
                <w:p><w:r><w:t>After table explanation.</w:t></w:r></w:p>
                """));

        KbIndexTask processed = indexService.processIndexTask(indexService.createPendingTask(docx).getId());

        assertThat(processed.getStatus()).isEqualTo(IndexTaskStatus.SUCCEEDED);
        List<KbDocChunk> chunks = chunksFor(docx.getId());
        assertThat(chunks).extracting(KbDocChunk::getContent)
                .containsExactly(
                        "Intro paragraph.",
                        "Concept | Definition; RAG | Retrieval augmented generation",
                        "After table explanation."
                );
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk.getMetadataJson())
                    .doesNotContain("Course RAG .... 1")
                    .doesNotContain("<w:tbl")
                    .doesNotContain("providerResponse")
                    .doesNotContain("rawOcrText");
        });
        assertThat(metadata(chunks.get(0)))
                .containsEntry("contentKind", "TEXT")
                .containsEntry("readingOrderIndex", 1)
                .containsEntry("pageNumSource", "PARSER_INFERRED");
        assertThat(metadata(chunks.get(1)))
                .containsEntry("contentKind", "TABLE_TEXT")
                .containsEntry("readingOrderIndex", 2)
                .containsEntry("pageNumSource", "PARSER_INFERRED");
        assertThat(metadata(chunks.get(2)))
                .containsEntry("contentKind", "TEXT")
                .containsEntry("readingOrderIndex", 3)
                .containsEntry("pageNumSource", "PARSER_INFERRED");
    }

    @Test
    void claimDuePendingTasksClaimsEachTaskOnlyOnceAndWritesLeaseState() {
        KbDocument document = saveDocument("claim.md", "text/markdown");
        KbIndexTask task = indexService.createPendingTask(document);
        Instant now = Instant.parse("2026-06-06T08:00:00Z");

        List<KbIndexTask> firstClaim = indexService.claimDuePendingTasks(
                now,
                1,
                "worker-a",
                Duration.ofMinutes(2)
        );
        List<KbIndexTask> secondClaim = indexService.claimDuePendingTasks(
                now.plusSeconds(1),
                1,
                "worker-b",
                Duration.ofMinutes(2)
        );

        assertThat(firstClaim).extracting(KbIndexTask::getId).containsExactly(task.getId());
        assertThat(secondClaim).isEmpty();
        assertThat(indexTaskRepository.findById(task.getId()))
                .hasValueSatisfying(claimed -> {
                    assertThat(claimed.getStatus()).isEqualTo(IndexTaskStatus.RUNNING);
                    assertThat(claimed.getProgressPercent()).isEqualTo(5);
                    assertThat(claimed.getProgressPhase()).isEqualTo("CLAIMED");
                    assertThat(claimed.getHeartbeatAt()).isEqualTo(now);
                    assertThat(claimed.getLeaseOwner()).isEqualTo("worker-a");
                    assertThat(claimed.getLeaseUntil()).isEqualTo(now.plus(Duration.ofMinutes(2)));
                });
    }

    @Test
    void failedProcessingRequeuesWithBackoffUntilRetryBudgetIsExhausted() {
        KbDocument document = saveDocument("poison.md", "text/markdown");
        KbIndexTask task = indexService.createPendingTask(document);

        KbIndexTask firstFailure = indexService.processIndexTask(
                task.getId(),
                "worker-a",
                2,
                Duration.ofSeconds(30),
                Duration.ofMinutes(2)
        );

        assertThat(firstFailure.getStatus()).isEqualTo(IndexTaskStatus.PENDING);
        assertThat(firstFailure.getRetryCount()).isEqualTo(1);
        assertThat(firstFailure.getProgressPhase()).isEqualTo("RETRY_WAIT");
        assertThat(firstFailure.getErrorMessage()).isEqualTo("DOCUMENT_EMPTY_OR_UNAVAILABLE");
        assertThat(firstFailure.getNextRetryAt()).isNotNull();
        assertThat(firstFailure.isRecoverable()).isTrue();

        indexService.processIndexTask(task.getId(), "worker-a", 2, Duration.ofSeconds(30), Duration.ofMinutes(2));
        KbIndexTask exhausted = indexService.processIndexTask(
                task.getId(),
                "worker-a",
                2,
                Duration.ofSeconds(30),
                Duration.ofMinutes(2)
        );

        assertThat(exhausted.getStatus()).isEqualTo(IndexTaskStatus.FAILED);
        assertThat(exhausted.getRetryCount()).isEqualTo(3);
        assertThat(exhausted.getProgressPhase()).isEqualTo("FAILED");
        assertThat(exhausted.getErrorMessage()).isEqualTo("DOCUMENT_EMPTY_OR_UNAVAILABLE");
        assertThat(exhausted.getNextRetryAt()).isNull();
        assertThat(exhausted.isRecoverable()).isFalse();
        assertThat(exhausted.getFinishedAt()).isNotNull();
    }

    private KbIndexTask saveTask(String documentId, IndexTaskStatus status, Instant updatedAt) {
        KbIndexTask task = new KbIndexTask();
        task.setDocumentId(documentId);
        task.setKbId("kb_sql");
        task.setStatus(status);
        task.setCreatedAt(updatedAt.minusSeconds(60));
        task.setUpdatedAt(updatedAt);
        return indexTaskRepository.saveAndFlush(task);
    }

    private List<KbDocChunk> chunksFor(String documentId) {
        return chunkRepository.findAll()
                .stream()
                .filter(chunk -> documentId.equals(chunk.getDocumentId()))
                .sorted(Comparator.comparing(KbDocChunk::getChunkIndex))
                .toList();
    }

    private Map<String, Object> metadata(KbDocChunk chunk) throws IOException {
        return objectMapper.readValue(chunk.getMetadataJson(), new TypeReference<>() {
        });
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

    private List<String> tokens(String content) {
        return Arrays.stream(content.split("\\s+"))
                .filter(token -> !token.isBlank())
                .toList();
    }

    private String numberedTokens(String prefix, int count) {
        List<String> values = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            values.add(prefix + String.format("%03d", i));
        }
        return String.join(" ", values);
    }

    private KbDocument saveDocument(String name) {
        return saveDocument(name, "text/markdown");
    }

    private KbDocument saveDocument(String name, String contentType) {
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
        document.setContentType(contentType);
        document.setSizeBytes(64L);
        document.setStorageBucket("learning-os-documents");
        document.setStorageKey(knowledgeBase.getId() + "/" + name);
        document.setCreatedBy("alice");
        return documentRepository.saveAndFlush(document);
    }

    private byte[] docxBytes(String text) throws IOException {
        return docxXmlBytes("<w:p><w:r><w:t>%s</w:t></w:r></w:p>".formatted(text));
    }

    private byte[] docxXmlBytes(String body) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write(("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                      <w:body>%s</w:body>
                    </w:document>
                    """.formatted(body)).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return output.toByteArray();
    }

    @TestConfiguration
    static class IndexServiceTestConfig {

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
        private volatile CountDownLatch readStarted;
        private volatile CountDownLatch releaseRead;

        @Override
        public StoredObject store(String kbId, MultipartFile file) throws IOException {
            String key = kbId + "/" + file.getOriginalFilename();
            objects.put(key, file.getBytes());
            return new StoredObject("learning-os-documents", key, file.getSize(), file.getContentType());
        }

        @Override
        public byte[] read(String bucket, String key) {
            CountDownLatch started = readStarted;
            CountDownLatch release = releaseRead;
            if (started != null && release != null) {
                started.countDown();
                try {
                    assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for blocked read", exception);
                }
            }
            return objects.get(bucket + "/" + key);
        }

        void put(KbDocument document, String content) {
            put(document, content.getBytes(StandardCharsets.UTF_8));
        }

        void put(KbDocument document, byte[] content) {
            objects.put(document.getStorageBucket() + "/" + document.getStorageKey(), content);
        }

        void blockReads() {
            readStarted = new CountDownLatch(1);
            releaseRead = new CountDownLatch(1);
        }

        boolean awaitReadStarted() throws InterruptedException {
            return readStarted != null && readStarted.await(5, TimeUnit.SECONDS);
        }

        void releaseReads() {
            if (releaseRead != null) {
                releaseRead.countDown();
            }
            readStarted = null;
            releaseRead = null;
        }
    }
}
