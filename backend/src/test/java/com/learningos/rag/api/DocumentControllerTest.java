package com.learningos.rag.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningos.knowledge.domain.CourseEnrollment;
import com.learningos.knowledge.repository.CourseEnrollmentRepository;
import com.learningos.rag.application.IndexService;
import com.learningos.rag.domain.enums.IndexTaskStatus;
import com.learningos.rag.repository.KbDocumentRepository;
import com.learningos.rag.repository.KbIndexTaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "learning-os.auth.jwt-secret=unit-test-secret-32-bytes-long!!",
        "learning-os.auth.issuer=learning-os"
})
class DocumentControllerTest {

    private static final String AUTH_SECRET = "unit-test-secret-32-bytes-long!!";
    private static final String AUTH_ISSUER = "learning-os";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final KbDocumentRepository documentRepository;
    private final KbIndexTaskRepository indexTaskRepository;
    private final IndexService indexService;
    private final CourseEnrollmentRepository courseEnrollmentRepository;

    DocumentControllerTest(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            KbDocumentRepository documentRepository,
            KbIndexTaskRepository indexTaskRepository,
            IndexService indexService,
            CourseEnrollmentRepository courseEnrollmentRepository
    ) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.documentRepository = documentRepository;
        this.indexTaskRepository = indexTaskRepository;
        this.indexService = indexService;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
    }

    @Test
    void uploadsDocumentCreatesPendingIndexTaskAndSupportsReindex() throws Exception {
        String teacherId = "teacher_alice";
        String kbId = createKnowledgeBase(teacherId, "PRIVATE");
        String courseId = createCourseForTeacher(teacherId, "SQL course");
        String chapterId = createChapterForTeacher(teacherId, courseId, "SQL JOIN");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "joins.md",
                "text/markdown",
                "JOIN duplicates often come from one-to-many relationships.".getBytes()
        );

        String uploadBody = mockMvc.perform(multipart("/api/knowledge-bases/{kbId}/documents", kbId)
                        .file(file)
                        .param("courseId", courseId)
                        .param("chapterId", chapterId)
                        .header("X-User-Id", teacherId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.documentId").isNotEmpty())
                .andExpect(jsonPath("$.data.indexTaskId").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode uploadJson = objectMapper.readTree(uploadBody);
        String documentId = uploadJson.path("data").path("documentId").asText();
        assertThat(documentId).isNotBlank();
        var document = documentRepository.findById(documentId).orElseThrow();
        assertThat(document.getStorageBucket()).isEqualTo("learning-os-documents");
        assertThat(document.getStorageKey()).startsWith(kbId + "/").endsWith("/joins.md");
        assertThat(document.getCourseId()).isEqualTo(courseId);
        assertThat(document.getChapterId()).isEqualTo(chapterId);
        assertThat(indexTaskRepository.findFirstByDocumentIdOrderByCreatedAtDesc(documentId)).isPresent();
        String firstIndexTaskId = uploadJson.path("data").path("indexTaskId").asText();

        mockMvc.perform(get("/api/documents/{documentId}", documentId).header("X-User-Id", teacherId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentId").value(documentId))
                .andExpect(jsonPath("$.data.parseStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.indexStatus").value("PENDING"));

        mockMvc.perform(post("/api/documents/{documentId}/reindex", documentId).header("X-User-Id", teacherId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentId").value(documentId))
                .andExpect(jsonPath("$.data.indexTaskId").value(firstIndexTaskId))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
        assertThat(indexTaskRepository.findAll().stream()
                .filter(task -> documentId.equals(task.getDocumentId()))
                .count()).isEqualTo(1);
    }

    @Test
    void replaysDocumentUploadWithSameRequestIdWithoutDuplicatingDocumentOrIndexTask() throws Exception {
        String teacherId = "teacher_alice";
        String kbId = createKnowledgeBase(teacherId, "PRIVATE");
        String courseId = createCourseForTeacher(teacherId, "SQL course");
        String chapterId = createChapterForTeacher(teacherId, courseId, "SQL JOIN");

        String firstBody = mockMvc.perform(multipart("/api/knowledge-bases/{kbId}/documents", kbId)
                        .file(documentFile("joins.md", "JOIN notes."))
                        .param("courseId", courseId)
                        .param("chapterId", chapterId)
                        .param("requestId", "req_upload_once")
                        .header("X-User-Id", teacherId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentId").isNotEmpty())
                .andExpect(jsonPath("$.data.indexTaskId").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode firstJson = objectMapper.readTree(firstBody);
        String documentId = firstJson.path("data").path("documentId").asText();
        String indexTaskId = firstJson.path("data").path("indexTaskId").asText();

        mockMvc.perform(multipart("/api/knowledge-bases/{kbId}/documents", kbId)
                        .file(documentFile("joins.md", "JOIN notes."))
                        .param("courseId", courseId)
                        .param("chapterId", chapterId)
                        .param("requestId", "req_upload_once")
                        .header("X-User-Id", teacherId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentId").value(documentId))
                .andExpect(jsonPath("$.data.indexTaskId").value(indexTaskId))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        assertThat(documentRepository.count()).isEqualTo(1);
        assertThat(indexTaskRepository.count()).isEqualTo(1);
        assertThat(documentRepository.findById(documentId))
                .hasValueSatisfying(document -> {
                    assertThat(document.getRequestId()).isEqualTo("req_upload_once");
                    assertThat(document.getRequestHash()).isNotBlank();
                    assertThat(document.getResponseJson()).contains(documentId, indexTaskId);
                });
    }

    @Test
    void replaysCourseBoundDocumentUploadWhenReplayOmitsCourseId() throws Exception {
        String teacherId = "teacher_alice";
        String kbId = createKnowledgeBase(teacherId, "PRIVATE");
        String courseId = createCourseForTeacher(teacherId, "SQL course");
        String chapterId = createChapterForTeacher(teacherId, courseId, "SQL JOIN");

        String firstBody = mockMvc.perform(multipart("/api/knowledge-bases/{kbId}/documents", kbId)
                        .file(documentFile("joins.md", "JOIN notes."))
                        .param("courseId", courseId)
                        .param("chapterId", chapterId)
                        .param("requestId", "req_upload_replay_omits_course")
                        .header("X-User-Id", teacherId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode firstJson = objectMapper.readTree(firstBody);
        String documentId = firstJson.path("data").path("documentId").asText();
        String indexTaskId = firstJson.path("data").path("indexTaskId").asText();

        mockMvc.perform(multipart("/api/knowledge-bases/{kbId}/documents", kbId)
                        .file(documentFile("joins.md", "JOIN notes."))
                        .param("chapterId", chapterId)
                        .param("requestId", "req_upload_replay_omits_course")
                        .header("X-User-Id", teacherId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentId").value(documentId))
                .andExpect(jsonPath("$.data.indexTaskId").value(indexTaskId))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        assertThat(documentRepository.count()).isEqualTo(1);
        assertThat(indexTaskRepository.count()).isEqualTo(1);
    }

    @Test
    void rejectsTeacherForeignCourseMetadataWithoutCreatingDocumentOrIndexTask() throws Exception {
        String teacherId = "teacher_alice";
        String kbId = createKnowledgeBase(teacherId, "PRIVATE");
        String foreignCourseId = createCourseForTeacher("teacher_bob", "Foreign SQL course");
        long documentCount = documentRepository.count();
        long indexTaskCount = indexTaskRepository.count();

        String body = mockMvc.perform(multipart("/api/knowledge-bases/{kbId}/documents", kbId)
                        .file(documentFile("foreign.md", "Foreign course pollution."))
                        .param("courseId", foreignCourseId)
                        .header("X-User-Id", teacherId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain(foreignCourseId);
        assertThat(documentRepository.count()).isEqualTo(documentCount);
        assertThat(indexTaskRepository.count()).isEqualTo(indexTaskCount);
    }

    @Test
    void rejectsTeacherMissingCourseMetadataWithoutEchoingCourseId() throws Exception {
        String teacherId = "teacher_alice";
        String kbId = createKnowledgeBase(teacherId, "PRIVATE");
        String missingCourseId = "course_missing_scope";
        long documentCount = documentRepository.count();
        long indexTaskCount = indexTaskRepository.count();

        String body = mockMvc.perform(multipart("/api/knowledge-bases/{kbId}/documents", kbId)
                        .file(documentFile("missing-course.md", "Missing course pollution."))
                        .param("courseId", missingCourseId)
                        .header("X-User-Id", teacherId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain(missingCourseId);
        assertThat(documentRepository.count()).isEqualTo(documentCount);
        assertThat(indexTaskRepository.count()).isEqualTo(indexTaskCount);
    }

    @Test
    void rejectsStudentCourseMetadataEvenWhenStudentOwnsKnowledgeBaseAndIsEnrolled() throws Exception {
        String studentId = "student_alice";
        String teacherId = "teacher_alice";
        String kbId = createKnowledgeBase(studentId, "PRIVATE");
        String courseId = createCourseForTeacher(teacherId, "Student enrolled course");
        enrollLearner(courseId, studentId);
        long documentCount = documentRepository.count();
        long indexTaskCount = indexTaskRepository.count();

        mockMvc.perform(multipart("/api/knowledge-bases/{kbId}/documents", kbId)
                        .file(documentFile("student.md", "Student should not upload course RAG."))
                        .param("courseId", courseId)
                        .header("X-User-Id", studentId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());

        assertThat(documentRepository.count()).isEqualTo(documentCount);
        assertThat(indexTaskRepository.count()).isEqualTo(indexTaskCount);
    }

    @Test
    void rejectsStudentCourseMetadataEvenWhenStudentOwnsPublicKnowledgeBaseAndIsEnrolled() throws Exception {
        String studentId = "student_public_owner";
        String teacherId = "teacher_public_course";
        String kbId = createKnowledgeBase(studentId, "PUBLIC");
        String courseId = createCourseForTeacher(teacherId, "Public KB student metadata guard");
        enrollLearner(courseId, studentId);
        long documentCount = documentRepository.count();
        long indexTaskCount = indexTaskRepository.count();

        String body = mockMvc.perform(multipart("/api/knowledge-bases/{kbId}/documents", kbId)
                        .file(documentFile("student-public.md", "Student cannot bind course metadata through public KB."))
                        .param("courseId", courseId)
                        .header("X-User-Id", studentId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain(courseId);
        assertThat(documentRepository.count()).isEqualTo(documentCount);
        assertThat(indexTaskRepository.count()).isEqualTo(indexTaskCount);
    }

    @Test
    void returnsNotFoundForAdminMissingCourseMetadata() throws Exception {
        String kbId = createKnowledgeBase("admin", "PRIVATE");
        long documentCount = documentRepository.count();
        long indexTaskCount = indexTaskRepository.count();

        mockMvc.perform(multipart("/api/knowledge-bases/{kbId}/documents", kbId)
                        .file(documentFile("admin-missing.md", "Admin missing course."))
                        .param("courseId", "course_missing_for_admin")
                        .header("X-User-Id", "admin"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.data").doesNotExist());

        assertThat(documentRepository.count()).isEqualTo(documentCount);
        assertThat(indexTaskRepository.count()).isEqualTo(indexTaskCount);
    }

    @Test
    void rejectsChapterMetadataWithoutCourseMetadata() throws Exception {
        String teacherId = "teacher_alice";
        String kbId = createKnowledgeBase(teacherId, "PRIVATE");
        long documentCount = documentRepository.count();
        long indexTaskCount = indexTaskRepository.count();

        mockMvc.perform(multipart("/api/knowledge-bases/{kbId}/documents", kbId)
                        .file(documentFile("chapter-only.md", "Chapter without course."))
                        .param("chapterId", "chapter_without_course")
                        .header("X-User-Id", teacherId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("courseId is required when chapterId is provided"))
                .andExpect(jsonPath("$.data").doesNotExist());

        assertThat(documentRepository.count()).isEqualTo(documentCount);
        assertThat(indexTaskRepository.count()).isEqualTo(indexTaskCount);
    }

    @Test
    void rejectsChapterThatDoesNotBelongToRequestCourseWithGenericValidationError() throws Exception {
        String teacherId = "teacher_alice";
        String kbId = createKnowledgeBase(teacherId, "PRIVATE");
        String courseId = createCourseForTeacher(teacherId, "SQL course");
        String otherCourseId = createCourseForTeacher(teacherId, "Other course");
        String otherChapterId = createChapterForTeacher(teacherId, otherCourseId, "Other chapter");
        long documentCount = documentRepository.count();
        long indexTaskCount = indexTaskRepository.count();

        String body = mockMvc.perform(multipart("/api/knowledge-bases/{kbId}/documents", kbId)
                        .file(documentFile("foreign-chapter.md", "Foreign chapter metadata."))
                        .param("courseId", courseId)
                        .param("chapterId", otherChapterId)
                        .header("X-User-Id", teacherId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("chapterId must belong to request course"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain(otherChapterId, otherCourseId);
        assertThat(documentRepository.count()).isEqualTo(documentCount);
        assertThat(indexTaskRepository.count()).isEqualTo(indexTaskCount);
    }

    @Test
    void rejectsMissingChapterWithGenericValidationError() throws Exception {
        String teacherId = "teacher_alice";
        String kbId = createKnowledgeBase(teacherId, "PRIVATE");
        String courseId = createCourseForTeacher(teacherId, "SQL course");
        String missingChapterId = "chapter_missing_scope";
        long documentCount = documentRepository.count();
        long indexTaskCount = indexTaskRepository.count();

        String body = mockMvc.perform(multipart("/api/knowledge-bases/{kbId}/documents", kbId)
                        .file(documentFile("missing-chapter.md", "Missing chapter metadata."))
                        .param("courseId", courseId)
                        .param("chapterId", missingChapterId)
                        .header("X-User-Id", teacherId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("chapterId must belong to request course"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain(missingChapterId);
        assertThat(documentRepository.count()).isEqualTo(documentCount);
        assertThat(indexTaskRepository.count()).isEqualTo(indexTaskCount);
    }

    @Test
    void rejectsSameRequestIdWhenCourseOrChapterMetadataChanges() throws Exception {
        String teacherId = "teacher_alice";
        String kbId = createKnowledgeBase(teacherId, "PRIVATE");
        String firstCourseId = createCourseForTeacher(teacherId, "First course");
        String firstChapterId = createChapterForTeacher(teacherId, firstCourseId, "First chapter");
        String secondCourseId = createCourseForTeacher(teacherId, "Second course");
        String secondChapterId = createChapterForTeacher(teacherId, secondCourseId, "Second chapter");

        mockMvc.perform(multipart("/api/knowledge-bases/{kbId}/documents", kbId)
                        .file(documentFile("joins.md", "JOIN notes."))
                        .param("courseId", firstCourseId)
                        .param("chapterId", firstChapterId)
                        .param("requestId", "req_upload_course_conflict")
                        .header("X-User-Id", teacherId))
                .andExpect(status().isOk());

        String body = mockMvc.perform(multipart("/api/knowledge-bases/{kbId}/documents", kbId)
                        .file(documentFile("joins.md", "JOIN notes."))
                        .param("courseId", secondCourseId)
                        .param("chapterId", secondChapterId)
                        .param("requestId", "req_upload_course_conflict")
                        .header("X-User-Id", teacherId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("requestId already used with different document upload payload"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain(secondCourseId, secondChapterId);
        assertThat(documentRepository.count()).isEqualTo(1);
        assertThat(indexTaskRepository.count()).isEqualTo(1);
    }

    @Test
    void rejectsDocumentUploadWhenSameRequestIdUsesDifferentPayload() throws Exception {
        String kbId = createKnowledgeBase();

        mockMvc.perform(multipart("/api/knowledge-bases/{kbId}/documents", kbId)
                        .file(documentFile("joins.md", "JOIN notes."))
                        .param("requestId", "req_upload_conflict")
                        .header("X-User-Id", "alice"))
                .andExpect(status().isOk());

        mockMvc.perform(multipart("/api/knowledge-bases/{kbId}/documents", kbId)
                        .file(documentFile("joins.md", "Different JOIN notes."))
                        .param("requestId", "req_upload_conflict")
                        .header("X-User-Id", "alice"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        assertThat(documentRepository.count()).isEqualTo(1);
        assertThat(indexTaskRepository.count()).isEqualTo(1);
    }

    @Test
    void reindexReturnsActiveRunningTaskAndCreatesNewTaskAfterTerminalStatus() throws Exception {
        String kbId = createKnowledgeBase();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "joins.md",
                "text/markdown",
                "JOIN notes.".getBytes()
        );
        String uploadBody = mockMvc.perform(multipart("/api/knowledge-bases/{kbId}/documents", kbId)
                        .file(file)
                        .header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode uploadJson = objectMapper.readTree(uploadBody);
        String documentId = uploadJson.path("data").path("documentId").asText();
        String firstIndexTaskId = uploadJson.path("data").path("indexTaskId").asText();

        var firstTask = indexTaskRepository.findById(firstIndexTaskId).orElseThrow();
        firstTask.setStatus(IndexTaskStatus.RUNNING);
        indexTaskRepository.saveAndFlush(firstTask);

        mockMvc.perform(post("/api/documents/{documentId}/reindex", documentId).header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentId").value(documentId))
                .andExpect(jsonPath("$.data.indexTaskId").value(firstIndexTaskId))
                .andExpect(jsonPath("$.data.status").value("RUNNING"));
        assertThat(indexTaskRepository.findAll().stream()
                .filter(task -> documentId.equals(task.getDocumentId()))
                .count()).isEqualTo(1);

        firstTask.setStatus(IndexTaskStatus.FAILED);
        indexTaskRepository.saveAndFlush(firstTask);

        String failedReindexBody = mockMvc.perform(post("/api/documents/{documentId}/reindex", documentId)
                        .header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentId").value(documentId))
                .andExpect(jsonPath("$.data.indexTaskId").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String secondIndexTaskId = objectMapper.readTree(failedReindexBody).path("data").path("indexTaskId").asText();
        assertThat(secondIndexTaskId).isNotEqualTo(firstIndexTaskId);
        assertThat(indexTaskRepository.findAll().stream()
                .filter(task -> documentId.equals(task.getDocumentId()))
                .count()).isEqualTo(2);

        var secondTask = indexTaskRepository.findById(secondIndexTaskId).orElseThrow();
        secondTask.setStatus(IndexTaskStatus.SUCCEEDED);
        indexTaskRepository.saveAndFlush(secondTask);

        String succeededReindexBody = mockMvc.perform(post("/api/documents/{documentId}/reindex", documentId)
                        .header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentId").value(documentId))
                .andExpect(jsonPath("$.data.indexTaskId").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String thirdIndexTaskId = objectMapper.readTree(succeededReindexBody).path("data").path("indexTaskId").asText();
        assertThat(thirdIndexTaskId).isNotEqualTo(secondIndexTaskId);
        assertThat(indexTaskRepository.findAll().stream()
                .filter(task -> documentId.equals(task.getDocumentId()))
                .count()).isEqualTo(3);
    }

    @Test
    void reindexCreatesNewPendingTaskAfterRunningTaskIsRecoveredAsTimedOut() throws Exception {
        String kbId = createKnowledgeBase();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "timeout.md",
                "text/markdown",
                "Timeout notes.".getBytes()
        );
        String uploadBody = mockMvc.perform(multipart("/api/knowledge-bases/{kbId}/documents", kbId)
                        .file(file)
                        .header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode uploadJson = objectMapper.readTree(uploadBody);
        String documentId = uploadJson.path("data").path("documentId").asText();
        String firstIndexTaskId = uploadJson.path("data").path("indexTaskId").asText();

        var firstTask = indexTaskRepository.findById(firstIndexTaskId).orElseThrow();
        firstTask.setStatus(IndexTaskStatus.RUNNING);
        java.time.Instant timeoutCutoff = java.time.Instant.now().plusSeconds(3600);
        firstTask.setLeaseUntil(timeoutCutoff.minusSeconds(1));
        firstTask.setUpdatedAt(timeoutCutoff.minusSeconds(1800));
        indexTaskRepository.saveAndFlush(firstTask);

        int recovered = indexService.recoverTimedOutRunningTasks(timeoutCutoff);
        assertThat(recovered).isEqualTo(1);
        assertThat(indexTaskRepository.findById(firstIndexTaskId))
                .hasValueSatisfying(task -> {
                    assertThat(task.getStatus()).isEqualTo(IndexTaskStatus.FAILED);
                    assertThat(task.getRetryCount()).isEqualTo(1);
                    assertThat(task.getErrorMessage()).isEqualTo("DOCUMENT_INDEX_LEASE_EXPIRED");
                    assertThat(task.getFinishedAt()).isNotNull();
                });

        String reindexBody = mockMvc.perform(post("/api/documents/{documentId}/reindex", documentId)
                        .header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentId").value(documentId))
                .andExpect(jsonPath("$.data.indexTaskId").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String secondIndexTaskId = objectMapper.readTree(reindexBody).path("data").path("indexTaskId").asText();
        assertThat(secondIndexTaskId).isNotEqualTo(firstIndexTaskId);
        assertThat(indexTaskRepository.findAll().stream()
                .filter(task -> documentId.equals(task.getDocumentId()))
                .count()).isEqualTo(2);
    }

    @Test
    void listsDocumentsForKnowledgeBase() throws Exception {
        String kbId = createKnowledgeBase();
        MockMultipartFile firstFile = new MockMultipartFile(
                "file",
                "joins.md",
                "text/markdown",
                "JOIN notes.".getBytes()
        );
        MockMultipartFile secondFile = new MockMultipartFile(
                "file",
                "indexes.pdf",
                "application/pdf",
                "PDF bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/knowledge-bases/{kbId}/documents", kbId)
                        .file(firstFile)
                        .header("X-User-Id", "alice"))
                .andExpect(status().isOk());
        mockMvc.perform(multipart("/api/knowledge-bases/{kbId}/documents", kbId)
                        .file(secondFile)
                        .header("X-User-Id", "alice"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/knowledge-bases/{kbId}/documents", kbId).header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].documentId").isNotEmpty())
                .andExpect(jsonPath("$.data[0].kbId").value(kbId))
                .andExpect(jsonPath("$.data[0].name").value("indexes.pdf"))
                .andExpect(jsonPath("$.data[0].parseStatus").value("PENDING"))
                .andExpect(jsonPath("$.data[0].indexStatus").value("PENDING"))
                .andExpect(jsonPath("$.data[1].name").value("joins.md"));
    }

    @Test
    void publicKnowledgeBaseIsReadableButNotWritableByOtherUsers() throws Exception {
        String kbId = createKnowledgeBase("alice", "PUBLIC");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "public.md",
                "text/markdown",
                "Public course notes.".getBytes()
        );

        String uploadBody = mockMvc.perform(multipart("/api/knowledge-bases/{kbId}/documents", kbId)
                        .file(file)
                        .header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String documentId = objectMapper.readTree(uploadBody).path("data").path("documentId").asText();

        mockMvc.perform(get("/api/documents/{documentId}", documentId).header("X-User-Id", "bob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentId").value(documentId));

        mockMvc.perform(post("/api/documents/{documentId}/reindex", documentId).header("X-User-Id", "bob"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        MockMultipartFile otherFile = new MockMultipartFile(
                "file",
                "bob.md",
                "text/markdown",
                "Bob should not be able to write here.".getBytes()
        );
        mockMvc.perform(multipart("/api/knowledge-bases/{kbId}/documents", kbId)
                        .file(otherFile)
                        .header("X-User-Id", "bob"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void returnsIndexTaskDetailForReadableKnowledgeBaseWithoutRawFailureDetails() throws Exception {
        String kbId = createKnowledgeBase();
        String uploadBody = mockMvc.perform(multipart("/api/knowledge-bases/{kbId}/documents", kbId)
                        .file(documentFile("empty.md", ""))
                        .header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode uploadJson = objectMapper.readTree(uploadBody);
        String indexTaskId = uploadJson.path("data").path("indexTaskId").asText();
        String documentId = uploadJson.path("data").path("documentId").asText();

        indexService.processIndexTask(indexTaskId);

        mockMvc.perform(get("/api/index-tasks/{taskId}", indexTaskId).header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value(indexTaskId))
                .andExpect(jsonPath("$.data.documentId").value(documentId))
                .andExpect(jsonPath("$.data.kbId").value(kbId))
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.progressPercent").isNumber())
                .andExpect(jsonPath("$.data.progressPhase").value("FAILED"))
                .andExpect(jsonPath("$.data.retryCount").isNumber())
                .andExpect(jsonPath("$.data.recoverable").value(false))
                .andExpect(jsonPath("$.data.errorMessage").value("DOCUMENT_EMPTY_OR_UNAVAILABLE"));
    }

    @Test
    void deniesIndexTaskDetailWhenUserCannotReadKnowledgeBase() throws Exception {
        String kbId = createKnowledgeBase("alice", "PRIVATE");
        String uploadBody = mockMvc.perform(multipart("/api/knowledge-bases/{kbId}/documents", kbId)
                        .file(documentFile("private.md", "Private course notes."))
                        .header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String indexTaskId = objectMapper.readTree(uploadBody).path("data").path("indexTaskId").asText();

        mockMvc.perform(get("/api/index-tasks/{taskId}", indexTaskId).header("X-User-Id", "bob"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void documentAndIndexTaskDetailsDoNotRevealMissingVersusForeignObjectsToNonAdmin() throws Exception {
        String kbId = createKnowledgeBase("alice", "PRIVATE");
        String uploadBody = mockMvc.perform(multipart("/api/knowledge-bases/{kbId}/documents", kbId)
                        .file(documentFile("private.md", "Private course notes."))
                        .header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String documentId = objectMapper.readTree(uploadBody).path("data").path("documentId").asText();
        String indexTaskId = objectMapper.readTree(uploadBody).path("data").path("indexTaskId").asText();

        String foreignDocumentBody = mockMvc.perform(get("/api/documents/{documentId}", documentId)
                        .header("X-User-Id", "bob"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String missingDocumentBody = mockMvc.perform(get("/api/documents/{documentId}", "doc_missing_object_scope")
                        .header("X-User-Id", "bob"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        mockMvc.perform(post("/api/documents/{documentId}/reindex", documentId)
                        .header("X-User-Id", "bob"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());

        String missingReindexBody = mockMvc.perform(post("/api/documents/{documentId}/reindex", "doc_missing_object_scope")
                        .header("X-User-Id", "bob"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String foreignIndexBody = mockMvc.perform(get("/api/index-tasks/{taskId}", indexTaskId)
                        .header("X-User-Id", "bob"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String missingIndexBody = mockMvc.perform(get("/api/index-tasks/{taskId}", "idx_missing_object_scope")
                        .header("X-User-Id", "bob"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(foreignDocumentBody).doesNotContain(documentId);
        assertThat(missingDocumentBody).doesNotContain("doc_missing_object_scope");
        assertThat(missingReindexBody).doesNotContain("doc_missing_object_scope");
        assertThat(foreignIndexBody).doesNotContain(indexTaskId);
        assertThat(missingIndexBody).doesNotContain("idx_missing_object_scope");
    }

    @Test
    void bearerAdminCanUploadDocumentToForeignPrivateKnowledgeBaseDespiteSpoofedUserIdHeader() throws Exception {
        String kbId = createKnowledgeBase("teacher_admin_target", "PRIVATE");
        long documentCount = documentRepository.count();
        long indexTaskCount = indexTaskRepository.count();

        mockMvc.perform(multipart("/api/knowledge-bases/{kbId}/documents", kbId)
                        .file(documentFile("admin-upload.md", "Admin can manage any active KB."))
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", "student_spoof"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.documentId").isNotEmpty())
                .andExpect(jsonPath("$.data.indexTaskId").isNotEmpty());

        assertThat(documentRepository.count()).isEqualTo(documentCount + 1);
        assertThat(indexTaskRepository.count()).isEqualTo(indexTaskCount + 1);
    }

    @Test
    void bearerTeacherCanUploadOwnCourseMetadataWithoutTeacherIdPrefix() throws Exception {
        String instructorId = "instructor_1";
        String kbId = createKnowledgeBase(instructorId, "PRIVATE");
        String courseId = createCourseAsAdminForTeacher(instructorId, "Instructor Bearer Course");

        mockMvc.perform(multipart("/api/knowledge-bases/{kbId}/documents", kbId)
                        .file(documentFile("instructor.md", "Instructor course metadata."))
                        .param("courseId", courseId)
                        .header("Authorization", "Bearer " + jwt(instructorId, "Instructor One", List.of("TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.documentId").isNotEmpty());
    }

    @Test
    void bearerUserSubjectTeacherPrefixCannotUploadCourseMetadata() throws Exception {
        String teacherId = "teacher_1";
        String kbId = createKnowledgeBase(teacherId, "PRIVATE");
        String courseId = createCourseForTeacher(teacherId, "Subject Prefix Is Not A Role");
        long documentCount = documentRepository.count();
        long indexTaskCount = indexTaskRepository.count();

        mockMvc.perform(multipart("/api/knowledge-bases/{kbId}/documents", kbId)
                        .file(documentFile("subject-prefix.md", "Subject name must not grant teacher role."))
                        .param("courseId", courseId)
                        .header("Authorization", "Bearer " + jwt(teacherId, "Fake Teacher", List.of("USER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());

        assertThat(documentRepository.count()).isEqualTo(documentCount);
        assertThat(indexTaskRepository.count()).isEqualTo(indexTaskCount);
    }

    @Test
    void bearerAdminMissingDocumentAndIndexTaskReturnNotFoundDespiteSpoofedHeader() throws Exception {
        String authorization = "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN"));

        mockMvc.perform(get("/api/documents/{documentId}", "doc_missing_bearer_admin")
                        .header("Authorization", authorization)
                        .header("X-User-Id", "student_spoof"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(post("/api/documents/{documentId}/reindex", "doc_missing_bearer_admin")
                        .header("Authorization", authorization)
                        .header("X-User-Id", "student_spoof"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/api/index-tasks/{taskId}", "idx_missing_bearer_admin")
                        .header("Authorization", authorization)
                        .header("X-User-Id", "student_spoof"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void bearerUserSubjectAdminMissingDocumentAndIndexTaskReturnForbidden() throws Exception {
        String authorization = "Bearer " + jwt("admin", "Subject Admin Only", List.of("USER"));

        mockMvc.perform(get("/api/documents/{documentId}", "doc_missing_subject_admin")
                        .header("Authorization", authorization))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(post("/api/documents/{documentId}/reindex", "doc_missing_subject_admin")
                        .header("Authorization", authorization))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/api/index-tasks/{taskId}", "idx_missing_subject_admin")
                        .header("Authorization", authorization))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void bearerTeacherCannotReindexForeignCourseBoundDocumentDespiteSpoofedHeaderWithoutSideEffects() throws Exception {
        String ownerTeacher = "teacher_reindex_owner";
        String foreignTeacher = "teacher_reindex_foreign";
        String kbId = createKnowledgeBase(ownerTeacher, "PRIVATE");
        String courseId = createCourseAsAdminForTeacher(ownerTeacher, "Owner reindex course");
        String uploadBody = mockMvc.perform(multipart("/api/knowledge-bases/{kbId}/documents", kbId)
                        .file(documentFile("owner-reindex.md", "Owner course-bound document."))
                        .param("courseId", courseId)
                        .header("X-User-Id", ownerTeacher))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String documentId = objectMapper.readTree(uploadBody).path("data").path("documentId").asText();
        String indexTaskId = objectMapper.readTree(uploadBody).path("data").path("indexTaskId").asText();
        long indexTaskCount = indexTaskRepository.count();

        String body = mockMvc.perform(post("/api/documents/{documentId}/reindex", documentId)
                        .header("Authorization", "Bearer " + jwt(foreignTeacher, "Foreign Teacher", List.of("TEACHER")))
                        .header("X-User-Id", ownerTeacher))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .doesNotContain(documentId)
                .doesNotContain(indexTaskId)
                .doesNotContain(courseId);
        assertThat(indexTaskRepository.count()).isEqualTo(indexTaskCount);
    }

    private String createKnowledgeBase() throws Exception {
        return createKnowledgeBase("alice", "PRIVATE");
    }

    private MockMultipartFile documentFile(String name, String content) {
        return new MockMultipartFile(
                "file",
                name,
                "text/markdown",
                content.getBytes()
        );
    }

    private String createKnowledgeBase(String userId, String visibility) throws Exception {
        String body = mockMvc.perform(post("/api/knowledge-bases")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "SQL course",
                                  "description": "Database lessons",
                                  "visibility": "%s"
                                }
                                """.formatted(visibility)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).path("data").path("id").asText();
    }

    private String createCourseForTeacher(String teacherId, String title) throws Exception {
        String body = mockMvc.perform(post("/api/courses")
                        .header("X-User-Id", teacherId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "description": "RAG document metadata scope.",
                                  "teacherId": "%s"
                                }
                                """.formatted(title, teacherId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).path("data").path("id").asText();
    }

    private String createCourseAsAdminForTeacher(String teacherId, String title) throws Exception {
        String body = mockMvc.perform(post("/api/courses")
                        .header("X-User-Id", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "description": "Admin seeded course for RAG metadata scope.",
                                  "teacherId": "%s"
                                }
                                """.formatted(title, teacherId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).path("data").path("id").asText();
    }

    private String createChapterForTeacher(String teacherId, String courseId, String title) throws Exception {
        String body = mockMvc.perform(post("/api/courses/{courseId}/chapters", courseId)
                        .header("X-User-Id", teacherId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "sequenceNo": 1
                                }
                                """.formatted(title)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).path("data").path("id").asText();
    }

    private void enrollLearner(String courseId, String learnerId) {
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setCourseId(courseId);
        enrollment.setLearnerId(learnerId);
        enrollment.setStatus("ACTIVE");
        courseEnrollmentRepository.saveAndFlush(enrollment);
    }

    private static String jwt(String sub, String name, List<String> roles) throws Exception {
        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String roleJson = roles.stream()
                .map(role -> "\"" + role + "\"")
                .collect(Collectors.joining(","));
        String payload = "{\"sub\":\"" + sub + "\",\"name\":\"" + name + "\",\"roles\":[" + roleJson
                + "],\"iss\":\"" + AUTH_ISSUER + "\",\"exp\":" + Instant.now().plusSeconds(3600).getEpochSecond() + "}";
        String signingInput = base64Url(header) + "." + base64Url(payload);
        return signingInput + "." + sign(signingInput);
    }

    private static String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String sign(String signingInput) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(AUTH_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII)));
    }
}
