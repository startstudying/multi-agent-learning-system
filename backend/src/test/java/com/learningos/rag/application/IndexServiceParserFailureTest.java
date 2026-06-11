package com.learningos.rag.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningos.rag.domain.KbDocument;
import com.learningos.rag.domain.KbIndexTask;
import com.learningos.rag.domain.KnowledgeBase;
import com.learningos.rag.domain.enums.DocumentStatus;
import com.learningos.rag.domain.enums.IndexTaskStatus;
import com.learningos.rag.domain.enums.Visibility;
import com.learningos.rag.parser.DocumentParseException;
import com.learningos.rag.parser.DocumentParserService;
import com.learningos.rag.parser.ParsedDocument;
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
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({
        IndexService.class,
        EmbeddingService.class,
        NoopVectorIndexAdapter.class,
        IndexServiceParserFailureTest.IndexServiceParserFailureTestConfig.class
})
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class IndexServiceParserFailureTest {

    private final IndexService indexService;
    private final KbDocumentRepository documentRepository;
    private final KbIndexTaskRepository indexTaskRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final TestDocumentStorageService storageService;

    IndexServiceParserFailureTest(
            IndexService indexService,
            KbDocumentRepository documentRepository,
            KbIndexTaskRepository indexTaskRepository,
            KnowledgeBaseRepository knowledgeBaseRepository,
            TestDocumentStorageService storageService
    ) {
        this.indexService = indexService;
        this.documentRepository = documentRepository;
        this.indexTaskRepository = indexTaskRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.storageService = storageService;
    }

    @Test
    void processIndexTaskConvertsParserFailureToSafeErrorCode() {
        KbDocument document = saveDocument("broken.md", "text/markdown");
        storageService.put(document, "ignored");
        KbIndexTask task = indexService.createPendingTask(document);

        KbIndexTask processed = indexService.processIndexTask(task.getId());

        assertThat(processed.getStatus()).isEqualTo(IndexTaskStatus.FAILED);
        assertThat(processed.getErrorMessage()).isEqualTo("DOCUMENT_PARSE_FAILED");
        assertThat(documentRepository.findById(document.getId()))
                .hasValueSatisfying(saved -> {
                    assertThat(saved.getParseStatus()).isEqualTo(DocumentStatus.FAILED);
                    assertThat(saved.getIndexStatus()).isEqualTo(DocumentStatus.FAILED);
                    assertThat(saved.getErrorMessage()).isEqualTo("DOCUMENT_PARSE_FAILED");
                });
        assertThat(indexTaskRepository.findById(task.getId()))
                .hasValueSatisfying(saved -> {
                    assertThat(saved.getStatus()).isEqualTo(IndexTaskStatus.FAILED);
                    assertThat(saved.getErrorMessage()).isEqualTo("DOCUMENT_PARSE_FAILED");
                    assertThat(saved.getFinishedAt()).isNotNull();
                });
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

    @TestConfiguration
    static class IndexServiceParserFailureTestConfig {

        @Bean
        TestDocumentStorageService documentStorageService() {
            return new TestDocumentStorageService();
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        @Primary
        DocumentParserService documentParserService() {
            return new DocumentParserService() {
                @Override
                public ParsedDocument parse(KbDocument document, byte[] bytes) {
                    throw new DocumentParseException(
                            "DOCUMENT_PARSE_FAILED",
                            new IOException("C:\\secret\\file.docx apiKey=sk-test raw learner text")
                    );
                }
            };
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
