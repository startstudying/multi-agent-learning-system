package com.learningos.agent.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningos.agent.repository.LearningResourceRepository;
import com.learningos.agent.repository.ResourceGenerationTaskRepository;
import com.learningos.agent.repository.ResourceReviewRepository;
import com.learningos.agent.domain.ResourceReview;
import com.learningos.knowledge.domain.Course;
import com.learningos.knowledge.domain.CourseEnrollment;
import com.learningos.knowledge.repository.CourseEnrollmentRepository;
import com.learningos.knowledge.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
class ResourceReviewControllerTest {

    private static final String AUTH_SECRET = "unit-test-secret-32-bytes-long!!";
    private static final String AUTH_ISSUER = "learning-os";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final ResourceReviewRepository resourceReviewRepository;
    private final LearningResourceRepository learningResourceRepository;
    private final ResourceGenerationTaskRepository resourceGenerationTaskRepository;
    private final CourseRepository courseRepository;
    @org.springframework.beans.factory.annotation.Autowired
    private CourseEnrollmentRepository courseEnrollmentRepository;

    ResourceReviewControllerTest(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            ResourceReviewRepository resourceReviewRepository,
            LearningResourceRepository learningResourceRepository,
            ResourceGenerationTaskRepository resourceGenerationTaskRepository,
            CourseRepository courseRepository
    ) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.resourceReviewRepository = resourceReviewRepository;
        this.learningResourceRepository = learningResourceRepository;
        this.resourceGenerationTaskRepository = resourceGenerationTaskRepository;
        this.courseRepository = courseRepository;
    }

    @Test
    void studentCannotListResourceReviews() throws Exception {
        String taskId = createGenerationTask();
        ResourceReview review = resourceReviewRepository.findAll().getFirst();

        String body = mockMvc.perform(get("/api/reviews/resources")
                        .header("X-User-Id", "alice")
                        .param("status", "PENDING_CRITIC"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .doesNotContain(taskId)
                .doesNotContain(review.getId())
                .doesNotContain(review.getResourceId());
    }

    @Test
    void studentCannotSubmitReviewDecision() throws Exception {
        String taskId = createGenerationTask();
        ResourceReview review = resourceReviewRepository.findAll().getFirst();

        String body = mockMvc.perform(post("/api/reviews/resources/{reviewId}/decision", review.getId())
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decision": "APPROVED",
                                  "summary": "Student should not be able to approve this resource."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .doesNotContain(taskId)
                .doesNotContain(review.getId())
                .doesNotContain(review.getResourceId());
    }

    @Test
    void adminCanListResourceReviews() throws Exception {
        String taskId = createGenerationTask();

        mockMvc.perform(get("/api/reviews/resources")
                        .header("X-User-Id", "admin")
                        .param("status", "PENDING_CRITIC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].generationTaskId").value(taskId))
                .andExpect(jsonPath("$.data[0].status").value("PENDING_CRITIC"));
    }

    @Test
    void adminCanSubmitReviewDecision() throws Exception {
        String taskId = createGenerationTask();
        ResourceReview review = resourceReviewRepository.findAll().getFirst();

        mockMvc.perform(post("/api/reviews/resources/{reviewId}/decision", review.getId())
                        .header("X-User-Id", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decision": "APPROVED",
                                  "summary": "Admin approved this resource."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.reviewId").value(review.getId()))
                .andExpect(jsonPath("$.data.generationTaskId").value(taskId))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.summary").value("Admin approved this resource."));
    }

    @Test
    void bearerAdminCanListAndDecideResourceReviewsDespiteSpoofedHeader() throws Exception {
        String taskId = createGenerationTask();
        ResourceReview review = resourceReviewRepository.findAll().getFirst();
        String token = jwt("ops_admin", "Ops Admin", List.of("ADMIN"));

        mockMvc.perform(get("/api/reviews/resources")
                        .header("Authorization", "Bearer " + token)
                        .header("X-User-Id", "alice")
                        .param("status", "PENDING_CRITIC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].generationTaskId").value(taskId));

        mockMvc.perform(post("/api/reviews/resources/{reviewId}/decision", review.getId())
                        .header("Authorization", "Bearer " + token)
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decision": "APPROVED",
                                  "summary": "Bearer admin approved this resource."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.reviewId").value(review.getId()))
                .andExpect(jsonPath("$.data.generationTaskId").value(taskId))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void bearerTeacherCanReviewOwnCourseWithoutTeacherSubjectPrefix() throws Exception {
        String taskId = createGenerationTask("alice", "course_bearer_teacher_own", "instructor_1");
        ResourceReview review = resourceReviewRepository.findAll().stream()
                .filter(existingReview -> taskId.equals(existingReview.getGenerationTaskId()))
                .findFirst()
                .orElseThrow();
        String token = jwt("instructor_1", "Instructor One", List.of("TEACHER"));

        mockMvc.perform(get("/api/reviews/resources")
                        .header("Authorization", "Bearer " + token)
                        .header("X-User-Id", "alice")
                        .param("status", "PENDING_CRITIC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].generationTaskId").value(taskId));

        mockMvc.perform(post("/api/reviews/resources/{reviewId}/decision", review.getId())
                        .header("Authorization", "Bearer " + token)
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decision": "APPROVED",
                                  "summary": "Bearer teacher approved own-course resource."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.reviewId").value(review.getId()))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void reviewListBearerTeacherNoPrefixRedactsForeignCourseReviews() throws Exception {
        String ownTaskId = createGenerationTask("alice", "course_bearer_teacher_list_own", "instructor_1");
        String foreignTaskId = createGenerationTask("bob", "course_bearer_teacher_list_foreign", "other_teacher");
        ResourceReview foreignReview = resourceReviewRepository.findAll().stream()
                .filter(existingReview -> foreignTaskId.equals(existingReview.getGenerationTaskId()))
                .findFirst()
                .orElseThrow();

        String body = mockMvc.perform(get("/api/reviews/resources")
                        .header("Authorization", "Bearer " + jwt("instructor_1", "Instructor One", List.of("TEACHER")))
                        .header("X-User-Id", "teacher_1")
                        .param("status", "PENDING_CRITIC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.length()").value(5))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .contains(ownTaskId)
                .doesNotContain(foreignTaskId)
                .doesNotContain(foreignReview.getId())
                .doesNotContain(foreignReview.getResourceId());
    }

    @Test
    void bearerUserSubjectAdminCannotListOrDecideResourceReviews() throws Exception {
        String taskId = createGenerationTask();
        ResourceReview review = resourceReviewRepository.findAll().getFirst();
        String token = jwt("admin", "Subject Admin", List.of("USER"));

        String listBody = mockMvc.perform(get("/api/reviews/resources")
                        .header("Authorization", "Bearer " + token)
                        .param("status", "PENDING_CRITIC"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(listBody)
                .doesNotContain(taskId)
                .doesNotContain(review.getId())
                .doesNotContain(review.getResourceId());

        String decisionBody = mockMvc.perform(post("/api/reviews/resources/{reviewId}/decision", review.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decision": "APPROVED",
                                  "summary": "Subject admin must not approve without ADMIN role."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(decisionBody)
                .doesNotContain(taskId)
                .doesNotContain(review.getId())
                .doesNotContain(review.getResourceId());
    }

    @Test
    void bearerUserSubjectTeacherCannotUseCourseTeacherIdForReviews() throws Exception {
        String taskId = createGenerationTask("alice", "course_subject_teacher_confusion", "teacher_1");
        ResourceReview review = resourceReviewRepository.findAll().stream()
                .filter(existingReview -> taskId.equals(existingReview.getGenerationTaskId()))
                .findFirst()
                .orElseThrow();
        String token = jwt("teacher_1", "Subject Teacher", List.of("USER"));

        String listBody = mockMvc.perform(get("/api/reviews/resources")
                        .header("Authorization", "Bearer " + token)
                        .param("status", "PENDING_CRITIC"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(listBody)
                .doesNotContain(taskId)
                .doesNotContain(review.getId())
                .doesNotContain(review.getResourceId());

        String decisionBody = mockMvc.perform(post("/api/reviews/resources/{reviewId}/decision", review.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decision": "APPROVED",
                                  "summary": "Subject teacher must not approve without TEACHER role."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(decisionBody)
                .doesNotContain(taskId)
                .doesNotContain(review.getId())
                .doesNotContain(review.getResourceId());
    }

    @Test
    void bearerTeacherCannotDistinguishMissingReviewFromForeignReview() throws Exception {
        createGenerationTask("alice", "course_bearer_teacher_anchor", "instructor_1");
        String foreignTaskId = createGenerationTask("bob", "course_bearer_teacher_foreign", "other_teacher");
        ResourceReview foreignReview = resourceReviewRepository.findAll().stream()
                .filter(existingReview -> foreignTaskId.equals(existingReview.getGenerationTaskId()))
                .findFirst()
                .orElseThrow();
        String token = jwt("instructor_1", "Instructor One", List.of("TEACHER"));

        String foreignBody = mockMvc.perform(post("/api/reviews/resources/{reviewId}/decision", foreignReview.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decision": "APPROVED",
                                  "summary": "Foreign review should be hidden from teacher."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String missingBody = mockMvc.perform(post("/api/reviews/resources/{reviewId}/decision", "missing_review")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decision": "APPROVED",
                                  "summary": "Missing review should not be distinguishable."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(foreignBody)
                .doesNotContain(foreignTaskId)
                .doesNotContain(foreignReview.getId())
                .doesNotContain(foreignReview.getResourceId());
        assertThat(missingBody)
                .doesNotContain("missing_review")
                .doesNotContain(foreignTaskId)
                .doesNotContain(foreignReview.getResourceId());
    }

    @Test
    void bearerTeacherWithSpoofedAdminHeaderCannotDistinguishMissingReviewFromForeignReviewAndDoesNotMutate() throws Exception {
        createGenerationTask("alice", "course_bearer_teacher_spoof_anchor", "instructor_1");
        String foreignTaskId = createGenerationTask("bob", "course_bearer_teacher_spoof_foreign", "other_teacher");
        ResourceReview foreignReview = resourceReviewRepository.findAll().stream()
                .filter(existingReview -> foreignTaskId.equals(existingReview.getGenerationTaskId()))
                .findFirst()
                .orElseThrow();
        String reviewStatus = foreignReview.getStatus();
        String resourceId = foreignReview.getResourceId();
        String resourceReviewStatus = learningResourceRepository.findById(resourceId).orElseThrow().getReviewStatus();
        String taskStatus = resourceGenerationTaskRepository.findById(foreignTaskId).orElseThrow().getStatus();
        String taskReviewStatus = resourceGenerationTaskRepository.findById(foreignTaskId).orElseThrow().getReviewStatus();
        String token = jwt("instructor_1", "Instructor One", List.of("TEACHER"));

        String foreignBody = mockMvc.perform(post("/api/reviews/resources/{reviewId}/decision", foreignReview.getId())
                        .header("Authorization", "Bearer " + token)
                        .header("X-User-Id", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decision": "APPROVED",
                                  "summary": "Spoofed admin header must not approve a foreign review."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String missingBody = mockMvc.perform(post("/api/reviews/resources/{reviewId}/decision", "missing_review_spoofed_admin")
                        .header("Authorization", "Bearer " + token)
                        .header("X-User-Id", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decision": "APPROVED",
                                  "summary": "Spoofed admin header must not reveal missing reviews."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(foreignBody)
                .doesNotContain(foreignTaskId)
                .doesNotContain(foreignReview.getId())
                .doesNotContain(resourceId)
                .doesNotContain("Spoofed admin header");
        assertThat(missingBody)
                .doesNotContain("missing_review_spoofed_admin")
                .doesNotContain(foreignTaskId)
                .doesNotContain(resourceId)
                .doesNotContain("Spoofed admin header");
        assertThat(resourceReviewRepository.findById(foreignReview.getId()))
                .hasValueSatisfying(review -> assertThat(review.getStatus()).isEqualTo(reviewStatus));
        assertThat(learningResourceRepository.findById(resourceId))
                .hasValueSatisfying(resource -> assertThat(resource.getReviewStatus()).isEqualTo(resourceReviewStatus));
        assertThat(resourceGenerationTaskRepository.findById(foreignTaskId))
                .hasValueSatisfying(task -> {
                    assertThat(task.getStatus()).isEqualTo(taskStatus);
                    assertThat(task.getReviewStatus()).isEqualTo(taskReviewStatus);
                });
    }

    @Test
    void listsPendingResourceReviewsAndApprovesAllResourcesForTask() throws Exception {
        String taskId = createGenerationTask();

        String listBody = mockMvc.perform(get("/api/reviews/resources")
                        .header("X-User-Id", "teacher")
                        .param("status", "PENDING_CRITIC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].reviewId").isNotEmpty())
                .andExpect(jsonPath("$.data[0].resourceId").isNotEmpty())
                .andExpect(jsonPath("$.data[0].generationTaskId").value(taskId))
                .andExpect(jsonPath("$.data[0].status").value("PENDING_CRITIC"))
                .andExpect(jsonPath("$.data[0].resourceReviewStatus").value("PENDING_CRITIC"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode reviews = objectMapper.readTree(listBody).path("data");
        for (JsonNode review : reviews) {
            mockMvc.perform(post("/api/reviews/resources/{reviewId}/decision", review.path("reviewId").asText())
                            .header("X-User-Id", "teacher")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "decision": "APPROVED",
                                      "summary": "Accurate and ready for learner release.",
                                      "reason": "Matches the course objective.",
                                      "citationCheck": "Citations are grounded in course material.",
                                      "safetyCheck": "No unsafe shortcut is present.",
                                      "revisionSuggestion": "No revision required."
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.reviewId").value(review.path("reviewId").asText()))
                    .andExpect(jsonPath("$.data.status").value("APPROVED"))
                    .andExpect(jsonPath("$.data.reason").value("Matches the course objective."))
                    .andExpect(jsonPath("$.data.citationCheck").value("Citations are grounded in course material."))
                    .andExpect(jsonPath("$.data.safetyCheck").value("No unsafe shortcut is present."))
                    .andExpect(jsonPath("$.data.revisionSuggestion").value("No revision required."));
        }

        assertThat(resourceReviewRepository.findAll())
                .allSatisfy(review -> assertThat(review.getStatus()).isEqualTo("APPROVED"));
        assertThat(learningResourceRepository.findByGenerationTaskIdOrderByCreatedAtAsc(taskId))
                .allSatisfy(resource -> assertThat(resource.getReviewStatus()).isEqualTo("PUBLISHED"));
        assertThat(resourceGenerationTaskRepository.findById(taskId))
                .hasValueSatisfying(task -> {
                    assertThat(task.getReviewStatus()).isEqualTo("PUBLISHED");
                    assertThat(task.getStatus()).isEqualTo("DONE");
                });
    }

    @Test
    void teacherListsOnlyReviewsForOwnCourses() throws Exception {
        String ownTaskId = createGenerationTask("alice", "course_teacher_scope", "teacher");
        String foreignTaskId = createGenerationTask("bob", "course_other_scope", "other_teacher");

        String body = mockMvc.perform(get("/api/reviews/resources")
                        .header("X-User-Id", "teacher")
                        .param("status", "PENDING_CRITIC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.length()").value(5))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .contains(ownTaskId)
                .doesNotContain(foreignTaskId);
    }

    @Test
    void teacherCannotDecideReviewForForeignCourse() throws Exception {
        String foreignTaskId = createGenerationTask("bob", "course_foreign_decision", "other_teacher");
        ResourceReview foreignReview = resourceReviewRepository.findAll().stream()
                .filter(review -> foreignTaskId.equals(review.getGenerationTaskId()))
                .findFirst()
                .orElseThrow();

        String body = mockMvc.perform(post("/api/reviews/resources/{reviewId}/decision", foreignReview.getId())
                        .header("X-User-Id", "teacher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decision": "APPROVED",
                                  "summary": "Teacher must not approve a foreign course review."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .doesNotContain(foreignTaskId)
                .doesNotContain(foreignReview.getResourceId());
    }

    @Test
    void teacherCannotDistinguishMissingReviewFromForbiddenReview() throws Exception {
        createGenerationTask("alice", "course_teacher_oracle_guard", "teacher");

        mockMvc.perform(post("/api/reviews/resources/{reviewId}/decision", "missing_review")
                        .header("X-User-Id", "teacher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decision": "APPROVED",
                                  "summary": "Missing review should not reveal existence semantics to teachers."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void revisionRequestUpdatesOnlyTargetResourceAndKeepsTaskPending() throws Exception {
        String taskId = createGenerationTask();
        String reviewId = resourceReviewRepository.findAll().getFirst().getId();
        String resourceId = resourceReviewRepository.findAll().getFirst().getResourceId();

        mockMvc.perform(post("/api/reviews/resources/{reviewId}/decision", reviewId)
                        .header("X-User-Id", "teacher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decision": "REVISION_REQUESTED",
                                  "summary": "Add stronger citations before approval."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewId").value(reviewId))
                .andExpect(jsonPath("$.data.status").value("REVISION_REQUESTED"))
                .andExpect(jsonPath("$.data.resourceReviewStatus").value("REVISION_REQUESTED"))
                .andExpect(jsonPath("$.data.summary").value("Add stronger citations before approval."));

        assertThat(learningResourceRepository.findById(resourceId))
                .hasValueSatisfying(resource -> assertThat(resource.getReviewStatus()).isEqualTo("REVISION_REQUESTED"));
        assertThat(resourceGenerationTaskRepository.findById(taskId))
                .hasValueSatisfying(task -> assertThat(task.getReviewStatus()).isEqualTo("REVISION_REQUESTED"));
    }

    @Test
    void rejectedDecisionPersistsStructuredAuditFieldsAndKeepsResourceUnpublished() throws Exception {
        String taskId = createGenerationTask();
        String reviewId = resourceReviewRepository.findAll().getFirst().getId();
        String resourceId = resourceReviewRepository.findAll().getFirst().getResourceId();

        mockMvc.perform(post("/api/reviews/resources/{reviewId}/decision", reviewId)
                        .header("X-User-Id", "teacher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decision": "REJECTED",
                                  "summary": "Cannot be released because citation evidence is missing.",
                                  "reason": "The explanation introduces unsupported claims.",
                                  "citationCheck": "No matching course citation was found.",
                                  "safetyCheck": "Safe content, but not grounded.",
                                  "revisionSuggestion": "Regenerate with course citations before another review."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewId").value(reviewId))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.resourceReviewStatus").value("REJECTED"))
                .andExpect(jsonPath("$.data.reason").value("The explanation introduces unsupported claims."))
                .andExpect(jsonPath("$.data.citationCheck").value("No matching course citation was found."))
                .andExpect(jsonPath("$.data.safetyCheck").value("Safe content, but not grounded."))
                .andExpect(jsonPath("$.data.revisionSuggestion").value("Regenerate with course citations before another review."));

        assertThat(resourceReviewRepository.findById(reviewId))
                .hasValueSatisfying(review -> {
                    assertThat(review.getStatus()).isEqualTo("REJECTED");
                    assertThat(review.getReason()).isEqualTo("The explanation introduces unsupported claims.");
                    assertThat(review.getCitationCheck()).isEqualTo("No matching course citation was found.");
                    assertThat(review.getSafetyCheck()).isEqualTo("Safe content, but not grounded.");
                    assertThat(review.getRevisionSuggestion()).isEqualTo("Regenerate with course citations before another review.");
                });
        assertThat(learningResourceRepository.findById(resourceId))
                .hasValueSatisfying(resource -> assertThat(resource.getReviewStatus()).isEqualTo("REJECTED"));
        assertThat(resourceGenerationTaskRepository.findById(taskId))
                .hasValueSatisfying(task -> assertThat(task.getReviewStatus()).isEqualTo("REJECTED"));
    }

    @Test
    void rejectsUnsupportedReviewDecision() throws Exception {
        String reviewId = resourceReviewRepository.findAll().isEmpty()
                ? createGenerationTaskAndReturnFirstReviewId()
                : resourceReviewRepository.findAll().getFirst().getId();

        mockMvc.perform(post("/api/reviews/resources/{reviewId}/decision", reviewId)
                        .header("X-User-Id", "teacher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decision": "ARCHIVED",
                                  "summary": "Unsupported decision for this workflow."
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private String createGenerationTaskAndReturnFirstReviewId() throws Exception {
        createGenerationTask();
        return resourceReviewRepository.findAll().getFirst().getId();
    }

    private String createGenerationTask() throws Exception {
        return createGenerationTask("alice", "goal_spring_boot", "teacher");
    }

    private String createGenerationTask(String learnerId, String goalId, String teacherId) throws Exception {
        seedCourse(goalId, teacherId);
        String body = mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("X-User-Id", learnerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "%s",
                                  "goalId": "%s",
                                  "pathNodeId": "node_sql_join",
                                  "resourceTypes": ["LECTURE", "EXERCISE"]
                                }
                                """.formatted(learnerId, goalId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).path("data").path("taskId").asText();
    }

    private void seedCourse(String courseId, String teacherId) {
        if (courseRepository.existsById(courseId)) {
            return;
        }
        Course course = new Course();
        course.setId(courseId);
        course.setTitle("Course " + courseId);
        course.setDescription("Course for review scope tests.");
        course.setTeacherId(teacherId);
        course.setStatus("PUBLISHED");
        courseRepository.save(course);
        seedEnrollment(courseId, "alice");
        seedEnrollment(courseId, "bob");
    }

    private void seedEnrollment(String courseId, String learnerId) {
        if (courseEnrollmentRepository.existsByCourseIdAndLearnerIdAndStatus(courseId, learnerId, "ACTIVE")) {
            return;
        }
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setCourseId(courseId);
        enrollment.setLearnerId(learnerId);
        enrollment.setStatus("ACTIVE");
        courseEnrollmentRepository.save(enrollment);
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
