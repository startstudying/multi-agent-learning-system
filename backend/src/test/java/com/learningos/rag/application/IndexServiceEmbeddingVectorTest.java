package com.learningos.rag.application;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
        IndexServiceEmbeddingVectorTest.TestConfig.class
})
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "learning-os.ai-model.provider=openai",
        "learning-os.ai-model.embedding-model=text-embedding-test"
})
class IndexServiceEmbeddingVectorTest {

    private final IndexService indexService;
    private final KbIndexTaskRepository indexTaskRepository;
    private final KbDocumentRepository documentRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KbDocChunkRepository chunkRepository;
    private final TestDocumentStorageService storageService;

    IndexServiceEmbeddingVectorTest(
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
    void embeddingProviderFailureDoesNotLeaveSearchableChunksOrRawError() {
        KbDocument document = saveDocument("embedding-provider.md");
        KbDocChunk oldChunk = new KbDocChunk();
        oldChunk.setKbId(document.getKbId());
        oldChunk.setDocumentId(document.getId());
        oldChunk.setDocumentVersion(document.getVersion());
        oldChunk.setChunkIndex(0);
        oldChunk.setContent("old searchable chunk");
        oldChunk.setChunkHash("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        chunkRepository.saveAndFlush(oldChunk);
        storageService.put(document, "# Embedding\n\nSQL JOIN duplicate rows need cardinality checks.");
        KbIndexTask task = indexService.createPendingTask(document);

        KbIndexTask processed = indexService.processIndexTask(task.getId());

        assertThat(processed.getStatus()).isEqualTo(IndexTaskStatus.FAILED);
        assertThat(processed.getErrorMessage()).isEqualTo("EMBEDDING_PROVIDER_NOT_CONFIGURED");
        assertThat(processed.getErrorMessage()).doesNotContain("SQL JOIN", "apiKey", "secret");
        assertThat(documentRepository.findById(document.getId()))
                .hasValueSatisfying(failedDocument -> {
                    assertThat(failedDocument.getIndexStatus()).isEqualTo(DocumentStatus.FAILED);
                    assertThat(failedDocument.getErrorMessage()).isEqualTo("EMBEDDING_PROVIDER_NOT_CONFIGURED");
                });
        assertThat(chunksFor(document.getId())).isEmpty();
    }

    private List<KbDocChunk> chunksFor(String documentId) {
        return chunkRepository.findAll().stream()
                .filter(chunk -> documentId.equals(chunk.getDocumentId()))
                .toList();
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

    @TestConfiguration
    static class TestConfig {

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
