package com.learningos.rag.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningos.rag.domain.KbDocChunk;
import com.learningos.rag.domain.KbDocument;
import com.learningos.rag.domain.KbIndexTask;
import com.learningos.rag.domain.KnowledgeBase;
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
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({
        IndexService.class,
        DocumentParserService.class,
        EmbeddingService.class,
        IndexServiceVectorPayloadTest.TestConfig.class
})
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "learning-os.ai-model.provider=openai",
        "learning-os.ai-model.embedding-model=text-embedding-test"
})
class IndexServiceVectorPayloadTest {

    private final IndexService indexService;
    private final KbDocumentRepository documentRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KbDocChunkRepository chunkRepository;
    private final TestDocumentStorageService storageService;
    private final CapturingVectorIndexAdapter vectorIndexAdapter;

    IndexServiceVectorPayloadTest(
            IndexService indexService,
            KbDocumentRepository documentRepository,
            KnowledgeBaseRepository knowledgeBaseRepository,
            KbDocChunkRepository chunkRepository,
            TestDocumentStorageService storageService,
            CapturingVectorIndexAdapter vectorIndexAdapter
    ) {
        this.indexService = indexService;
        this.documentRepository = documentRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.chunkRepository = chunkRepository;
        this.storageService = storageService;
        this.vectorIndexAdapter = vectorIndexAdapter;
    }

    @Test
    void vectorUpsertReceivesEmbeddingVectorsWithoutRawChunkContent() {
        KbDocument document = saveDocument("vector-payload.md");
        storageService.put(document, "# Vector\n\nSQL JOIN vector payload contract.");
        KbIndexTask task = indexService.createPendingTask(document);

        KbIndexTask processed = indexService.processIndexTask(task.getId());

        assertThat(processed.getStatus()).isEqualTo(IndexTaskStatus.SUCCEEDED);
        VectorUpsertRequest request = vectorIndexAdapter.capturedUpsert().get();
        assertThat(request).isNotNull();
        assertThat(request.kbId()).isEqualTo(document.getKbId());
        assertThat(request.documentId()).isEqualTo(document.getId());
        assertThat(request.chunks()).isNotEmpty();
        for (VectorChunkReference reference : request.chunks()) {
            assertThat(reference.vector()).isNotNull();
            assertThat(reference.vector().ownerId()).isEqualTo(reference.chunkId());
            assertThat(reference.vector().values()).hasSize(3);
        }
        assertThat(request.toString())
                .doesNotContain("SQL JOIN", "vector payload", "0.11", "0.22", "0.33", "secret", "apiKey");
        assertThat(chunkRepository.findAll())
                .filteredOn(chunk -> document.getId().equals(chunk.getDocumentId()))
                .isNotEmpty()
                .allSatisfy(chunk -> {
                    assertThat(chunk.getMetadataJson()).contains("\"vectorIndexStatus\":\"SUCCEEDED\"");
                    assertThat(chunk.getMetadataJson()).doesNotContain("0.11", "0.22", "0.33", "vector payload");
                });
    }

    private KbDocument saveDocument(String name) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setName("Vector course");
        knowledgeBase.setDescription("Vector lessons");
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

        @Bean
        CapturingVectorIndexAdapter vectorIndexAdapter() {
            return new CapturingVectorIndexAdapter();
        }

        @Bean
        EmbeddingModel embeddingModel() {
            return new DeterministicEmbeddingModel();
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

    static class CapturingVectorIndexAdapter implements VectorIndexAdapter {

        private final AtomicReference<VectorUpsertRequest> capturedUpsert = new AtomicReference<>();

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public VectorUpsertResult deleteDocument(String kbId, String documentId, int documentVersion) {
            return new VectorUpsertResult(VectorIndexStatus.SUCCEEDED, 0, 0L, null);
        }

        @Override
        public VectorUpsertResult upsert(VectorUpsertRequest request) {
            capturedUpsert.set(request);
            return new VectorUpsertResult(VectorIndexStatus.SUCCEEDED, request.chunks().size(), 0L, null);
        }

        @Override
        public VectorSearchResult search(VectorSearchRequest request) {
            return VectorSearchResult.succeeded(List.of());
        }

        AtomicReference<VectorUpsertRequest> capturedUpsert() {
            return capturedUpsert;
        }
    }

    static class DeterministicEmbeddingModel implements EmbeddingModel {

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<Embedding> embeddings = IntStream.range(0, request.getInstructions().size())
                    .mapToObj(index -> new Embedding(
                            new float[]{0.11f + index, 0.22f + index, 0.33f + index},
                            index
                    ))
                    .toList();
            return new EmbeddingResponse(embeddings);
        }

        @Override
        public float[] embed(org.springframework.ai.document.Document document) {
            return new float[0];
        }
    }
}
