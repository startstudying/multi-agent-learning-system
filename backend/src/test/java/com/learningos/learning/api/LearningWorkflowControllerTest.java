package com.learningos.learning.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningos.knowledge.domain.CourseEnrollment;
import com.learningos.knowledge.repository.CourseEnrollmentRepository;
import com.learningos.learning.domain.LearnerProfile;
import com.learningos.learning.domain.MasteryRecord;
import com.learningos.learning.repository.LearnerProfileRepository;
import com.learningos.learning.repository.LearningEventRepository;
import com.learningos.learning.repository.LearningPathNodeRepository;
import com.learningos.learning.repository.LearningPathRepository;
import com.learningos.learning.repository.MasteryRecordRepository;
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
class LearningWorkflowControllerTest {

    private static final String AUTH_SECRET = "unit-test-secret-32-bytes-long!!";
    private static final String AUTH_ISSUER = "learning-os";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final LearnerProfileRepository learnerProfileRepository;
    private final LearningPathRepository learningPathRepository;
    private final LearningPathNodeRepository learningPathNodeRepository;
    private final LearningEventRepository learningEventRepository;
    private final MasteryRecordRepository masteryRecordRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;

    LearningWorkflowControllerTest(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            LearnerProfileRepository learnerProfileRepository,
            LearningPathRepository learningPathRepository,
            LearningPathNodeRepository learningPathNodeRepository,
            LearningEventRepository learningEventRepository,
            MasteryRecordRepository masteryRecordRepository,
            CourseEnrollmentRepository courseEnrollmentRepository
    ) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.learnerProfileRepository = learnerProfileRepository;
        this.learningPathRepository = learningPathRepository;
        this.learningPathNodeRepository = learningPathNodeRepository;
        this.learningEventRepository = learningEventRepository;
        this.masteryRecordRepository = masteryRecordRepository;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
    }

    @Test
    void extractsProfileDraftAndGeneratesTraceableLearningPath() throws Exception {
        mockMvc.perform(post("/api/profile/dialogue/extract")
                        .header("X-User-Id", "learner_1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "learner_1",
                                  "message": "I want to learn Spring Boot but SQL joins confuse me."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.profileDraft.learnerId").value("learner_1"))
                .andExpect(jsonPath("$.data.profileDraft.dimensions.length()").value(7))
                .andExpect(jsonPath("$.data.profileDraft.dimensions[0].lastEvidenceId").isNotEmpty())
                .andExpect(jsonPath("$.data.profileDraft.structuredProfile.baseline_level").isNotEmpty())
                .andExpect(jsonPath("$.data.profileDraft.structuredProfile.learning_goal").isNotEmpty())
                .andExpect(jsonPath("$.data.profileDraft.structuredProfile.weak_point.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.data.profileDraft.structuredProfile.preference.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.data.profileDraft.structuredProfile.pace_and_feedback").isNotEmpty())
                .andExpect(jsonPath("$.data.profileDraft.structuredProfile.recent_error_pattern").isNotEmpty())
                .andExpect(jsonPath("$.data.profileDraft.structuredProfile.teacher_note").isNotEmpty())
                .andExpect(jsonPath("$.data.profileDraft.updatePolicy").value("LEARN_AS_YOU_GO"))
                .andExpect(jsonPath("$.data.followUpQuestions.length()").value(2))
                .andExpect(jsonPath("$.data.traceId").isNotEmpty());

        assertThat(learnerProfileRepository.countByLearnerId("learner_1")).isEqualTo(1);
        assertThat(learnerProfileRepository.findFirstByLearnerIdOrderByUpdatedAtDesc("learner_1"))
                .hasValueSatisfying(profile -> {
                    assertThat(profile.getDimensionsJson()).contains("baseline_level", "learning_goal", "preference");
                    assertThat(profile.getWeakPointsJson()).contains("SQL JOIN reasoning");
                    assertThat(profile.getDimensionsJson()).contains("lastEvidenceId", "recent_error_pattern", "teacher_note");
                });
        assertThat(learningEventRepository.countByLearnerId("learner_1")).isEqualTo(1);

        String pathBody = mockMvc.perform(post("/api/learning-paths")
                        .header("X-User-Id", "learner_1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "learner_1",
                                  "goalId": "goal_spring_boot"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.pathId").isNotEmpty())
                .andExpect(jsonPath("$.data.profileSnapshot").isNotEmpty())
                .andExpect(jsonPath("$.data.profileSnapshot").value(org.hamcrest.Matchers.containsString("baseline_level")))
                .andExpect(jsonPath("$.data.profileSnapshot").value(org.hamcrest.Matchers.containsString("recent_error_pattern")))
                .andExpect(jsonPath("$.data.nodes.length()").value(3))
                .andExpect(jsonPath("$.data.nodes[0].reasonSummary").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String pathId = objectMapper.readTree(pathBody).path("data").path("pathId").asText();
        assertThat(pathId).isNotBlank();
        assertThat(learningPathRepository.findById(pathId)).isPresent();
        assertThat(learningPathNodeRepository.countByPathId(pathId)).isEqualTo(3);
        assertThat(learningEventRepository.countByLearnerId("learner_1")).isEqualTo(2);

        mockMvc.perform(get("/api/learning-paths/{pathId}", pathId).header("X-User-Id", "learner_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.pathId").value(pathId))
                .andExpect(jsonPath("$.data.profileSnapshot").value(objectMapper.readTree(pathBody).path("data").path("profileSnapshot").asText()))
                .andExpect(jsonPath("$.data.reasonSummary").isNotEmpty());
    }

    @Test
    void profileExtractRejectsCrossLearnerAccessAndDoesNotPersist() throws Exception {
        mockMvc.perform(post("/api/profile/dialogue/extract")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "bob",
                                  "message": "I want to learn Spring Boot."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        assertThat(learnerProfileRepository.countByLearnerId("bob")).isZero();
        assertThat(learningEventRepository.countByLearnerId("bob")).isZero();
    }

    @Test
    void learningPathCreateRejectsCrossLearnerAccessAndDoesNotPersist() throws Exception {
        mockMvc.perform(post("/api/learning-paths")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "bob",
                                  "goalId": "goal_spring_boot"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        assertThat(learningPathRepository.findAll())
                .noneMatch(path -> "bob".equals(path.getLearnerId()));
        assertThat(learningPathNodeRepository.findAll())
                .noneMatch(node -> "bob".equals(node.getLearnerId()));
        assertThat(learningEventRepository.countByLearnerId("bob")).isZero();
    }

    @Test
    void learningPathGetRejectsNonOwnerWithoutExposingPathData() throws Exception {
        String pathBody = mockMvc.perform(post("/api/learning-paths")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "alice",
                                  "goalId": "goal_spring_boot"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String pathId = objectMapper.readTree(pathBody).path("data").path("pathId").asText();

        mockMvc.perform(get("/api/learning-paths/{pathId}", pathId)
                        .header("X-User-Id", "bob"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void learningPathGetDoesNotRevealMissingVersusForeignPathToNonAdmin() throws Exception {
        String pathBody = mockMvc.perform(post("/api/learning-paths")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "alice",
                                  "goalId": "goal_spring_boot"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String pathId = objectMapper.readTree(pathBody).path("data").path("pathId").asText();

        String foreignBody = mockMvc.perform(get("/api/learning-paths/{pathId}", pathId)
                        .header("X-User-Id", "bob"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String missingBody = mockMvc.perform(get("/api/learning-paths/{pathId}", "path_missing_object_scope")
                        .header("X-User-Id", "bob"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(foreignBody).doesNotContain(pathId);
        assertThat(missingBody).doesNotContain("path_missing_object_scope");
    }

    @Test
    void learningPathDetailUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader() throws Exception {
        String pathId = createTemplatePath("detail_admin_alice", "goal_detail_admin");

        mockMvc.perform(get("/api/learning-paths/{pathId}", pathId)
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", "bob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.pathId").value(pathId))
                .andExpect(jsonPath("$.data.learnerId").value("detail_admin_alice"));
    }

    @Test
    void learningPathDetailRejectsBearerUserSubjectAdminRoleConfusion() throws Exception {
        String pathId = createTemplatePath("detail_subject_admin_alice", "goal_detail_subject_admin");

        String responseBody = mockMvc.perform(get("/api/learning-paths/{pathId}", pathId)
                        .header("Authorization", "Bearer " + jwt("admin", "Subject Admin", List.of("USER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody).doesNotContain(pathId);
        assertThat(responseBody).doesNotContain("detail_subject_admin_alice");
        assertThat(responseBody).doesNotContain("goal_detail_subject_admin");
    }

    @Test
    void learningPathDetailBearerAdminMissingPathReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/learning-paths/{pathId}", "path_missing_roles_first_admin")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", "alice"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void learningPathDetailRejectsBearerUserSubjectAdminMissingPathAsForbidden() throws Exception {
        String responseBody = mockMvc.perform(get("/api/learning-paths/{pathId}", "path_missing_subject_admin")
                        .header("Authorization", "Bearer " + jwt("admin", "Subject Admin", List.of("USER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody).doesNotContain("path_missing_subject_admin");
    }

    @Test
    void learningPathDetailAllowsBearerOwnerDespiteSpoofedUserIdHeader() throws Exception {
        String pathId = createTemplatePath("detail_owner_alice", "goal_detail_owner");

        mockMvc.perform(get("/api/learning-paths/{pathId}", pathId)
                        .header("Authorization", "Bearer " + jwt("detail_owner_alice", "Owner Alice", List.of("USER")))
                        .header("X-User-Id", "bob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.pathId").value(pathId))
                .andExpect(jsonPath("$.data.learnerId").value("detail_owner_alice"));
    }

    @Test
    void learningPathDetailRejectsBearerNonOwnerForeignPathAsSafeForbidden() throws Exception {
        String pathId = createTemplatePath("detail_foreign_alice", "goal_detail_foreign");

        String responseBody = mockMvc.perform(get("/api/learning-paths/{pathId}", pathId)
                        .header("Authorization", "Bearer " + jwt("bob", "Bob", List.of("USER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody).doesNotContain(pathId);
        assertThat(responseBody).doesNotContain("detail_foreign_alice");
        assertThat(responseBody).doesNotContain("goal_detail_foreign");
    }

    @Test
    void learningPathDetailRejectsBearerNonOwnerMissingPathAsSafeForbidden() throws Exception {
        String responseBody = mockMvc.perform(get("/api/learning-paths/{pathId}", "path_missing_bearer_non_owner")
                        .header("Authorization", "Bearer " + jwt("bob", "Bob", List.of("USER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody).doesNotContain("path_missing_bearer_non_owner");
    }

    @Test
    void mergesProfileUpdatesFromConversationAssessmentResourceStudyAndTeacherNote() throws Exception {
        postProfileUpdate(
                "learner_sources",
                "CONVERSATION",
                "I want Spring Boot APIs and I like code labs."
        );
        postProfileUpdate(
                "learner_sources",
                "ASSESSMENT",
                "Assessment result: missed SQL JOIN cardinality questions."
        );
        postProfileUpdate(
                "learner_sources",
                "RESOURCE_STUDY",
                "Resource study: slowly completed diagrams and got stuck on RAG citations."
        );
        postProfileUpdate(
                "learner_sources",
                "TEACHER_NOTE",
                "Teacher note: learner needs transaction boundary practice and direct feedback."
        );

        assertThat(learnerProfileRepository.countByLearnerId("learner_sources")).isEqualTo(1);
        assertThat(learnerProfileRepository.findFirstByLearnerIdOrderByUpdatedAtDesc("learner_sources"))
                .hasValueSatisfying(profile -> {
                    assertThat(profile.getDimensionsJson())
                            .contains(
                                    "CONVERSATION",
                                    "ASSESSMENT",
                                    "RESOURCE_STUDY",
                                    "TEACHER_NOTE",
                                    "baseline_level",
                                    "learning_goal",
                                    "preference",
                                    "weak_point",
                                    "pace_and_feedback",
                                    "recent_error_pattern",
                                    "teacher_note",
                                    "lastEvidenceId"
                            );
                    assertThat(profile.getWeakPointsJson())
                            .contains("SQL JOIN reasoning", "RAG grounding", "transaction boundaries");
                    assertThat(profile.getPreferencesJson())
                            .contains("code labs", "diagrams", "direct feedback");
                });
    }

    @Test
    void learningPathSnapshotKeepsHistoricalProfileFieldAliases() throws Exception {
        LearnerProfile historicalProfile = new LearnerProfile();
        historicalProfile.setLearnerId("learner_legacy_profile");
        historicalProfile.setTarget("Legacy Spring Boot goal");
        historicalProfile.setWeakPointsJson("[\"Legacy SQL weak point\"]");
        historicalProfile.setPreferencesJson("[\"Legacy diagram preference\"]");
        historicalProfile.setDimensionsJson("""
                {
                  "dimensions": [
                    {
                      "name": "knowledge_base",
                      "value": "Legacy Java basics",
                      "confidence": 0.82,
                      "evidence": "historical baseline",
                      "sourceType": "CONVERSATION"
                    },
                    {
                      "name": "resource_preference",
                      "value": "Legacy code labs",
                      "confidence": 0.79,
                      "evidence": "historical resource preference",
                      "sourceType": "CONVERSATION"
                    },
                    {
                      "name": "error_pattern",
                      "value": "Legacy JOIN mistakes",
                      "confidence": 0.84,
                      "evidence": "historical assessment",
                      "sourceType": "ASSESSMENT"
                    }
                  ],
                  "sources": ["CONVERSATION", "ASSESSMENT"]
                }
                """);
        historicalProfile.setUpdatePolicy("LEARN_AS_YOU_GO");
        historicalProfile.setTraceId("trace_legacy_profile");
        learnerProfileRepository.save(historicalProfile);

        mockMvc.perform(post("/api/learning-paths")
                        .header("X-User-Id", "learner_legacy_profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "learner_legacy_profile",
                                  "goalId": "goal_spring_boot"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileSnapshot").value(org.hamcrest.Matchers.containsString("Legacy Java basics")))
                .andExpect(jsonPath("$.data.profileSnapshot").value(org.hamcrest.Matchers.containsString("Legacy code labs")))
                .andExpect(jsonPath("$.data.profileSnapshot").value(org.hamcrest.Matchers.containsString("Legacy SQL weak point")))
                .andExpect(jsonPath("$.data.profileSnapshot").value(org.hamcrest.Matchers.containsString("Legacy diagram preference")))
                .andExpect(jsonPath("$.data.profileSnapshot").value(org.hamcrest.Matchers.containsString("Legacy JOIN mistakes")));
    }

    @Test
    void generatesLearningPathFromCourseKnowledgeDagAndMasteryRecords() throws Exception {
        String courseId = createCourse();
        String chapterId = createChapter(courseId);
        String entityMappingId = createKnowledgePoint(courseId, chapterId, "Entity Mapping", 0.30);
        String repositoryServicesId = createKnowledgePoint(courseId, chapterId, "Repository Services", 0.55);
        String transactionBoundaryId = createKnowledgePoint(courseId, chapterId, "Transactional Boundaries", 0.70);
        createDependency(repositoryServicesId, entityMappingId);
        createDependency(transactionBoundaryId, repositoryServicesId);
        seedEnrollment(courseId, "learner_dag", "ACTIVE");
        saveMastery("learner_dag", entityMappingId, 0.86);

        String pathBody = mockMvc.perform(post("/api/learning-paths")
                        .header("X-User-Id", "learner_dag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "learner_dag",
                                  "goalId": "%s"
                                }
                                """.formatted(courseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.goalId").value(courseId))
                .andExpect(jsonPath("$.data.reasonSummary").value(org.hamcrest.Matchers.containsString("Knowledge DAG")))
                .andExpect(jsonPath("$.data.nodes.length()").value(3))
                .andExpect(jsonPath("$.data.nodes[0].nodeId").value(entityMappingId))
                .andExpect(jsonPath("$.data.nodes[0].status").value("DONE"))
                .andExpect(jsonPath("$.data.nodes[0].mastery").value(0.86))
                .andExpect(jsonPath("$.data.nodes[0].recommendationReason").isNotEmpty())
                .andExpect(jsonPath("$.data.nodes[0].estimatedDurationMinutes").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.data.nodes[0].resourceType").isNotEmpty())
                .andExpect(jsonPath("$.data.nodes[0].assessmentBindingRelation").isNotEmpty())
                .andExpect(jsonPath("$.data.nodes[1].nodeId").value(repositoryServicesId))
                .andExpect(jsonPath("$.data.nodes[1].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.nodes[1].reasonSummary").value(org.hamcrest.Matchers.containsString("prerequisites are satisfied")))
                .andExpect(jsonPath("$.data.nodes[2].nodeId").value(transactionBoundaryId))
                .andExpect(jsonPath("$.data.nodes[2].status").value("LOCKED"))
                .andExpect(jsonPath("$.data.nodes[2].reasonSummary").value(org.hamcrest.Matchers.containsString("Repository Services")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var data = objectMapper.readTree(pathBody).path("data");
        String pathId = data.path("pathId").asText();
        var firstNode = data.path("nodes").get(0);
        mockMvc.perform(get("/api/learning-paths/{pathId}", pathId).header("X-User-Id", "learner_dag"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nodes[0].recommendationReason").value(firstNode.path("recommendationReason").asText()))
                .andExpect(jsonPath("$.data.nodes[0].estimatedDurationMinutes").value(firstNode.path("estimatedDurationMinutes").asInt()))
                .andExpect(jsonPath("$.data.nodes[0].resourceType").value(firstNode.path("resourceType").asText()))
                .andExpect(jsonPath("$.data.nodes[0].assessmentBindingRelation").value(firstNode.path("assessmentBindingRelation").asText()));
    }

    @Test
    void ignoresRelatedAndAdvancedDependenciesWhenPlanningKnowledgeDagPath() throws Exception {
        String courseId = createCourse();
        String chapterId = createChapter(courseId);
        String relatedConceptId = createKnowledgePoint(courseId, chapterId, "Repository Patterns", 0.45);
        String entryPointId = createKnowledgePoint(courseId, chapterId, "Entity Mapping", 0.30);
        String advancedConceptId = createKnowledgePoint(courseId, chapterId, "Transactional Boundaries", 0.70);
        createDependency(relatedConceptId, entryPointId, "RELATED");
        createDependency(advancedConceptId, relatedConceptId, "ADVANCED");
        seedEnrollment(courseId, "learner_relation_types", "ACTIVE");

        mockMvc.perform(post("/api/learning-paths")
                        .header("X-User-Id", "learner_relation_types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "learner_relation_types",
                                  "goalId": "%s"
                                }
                                """.formatted(courseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.nodes.length()").value(3))
                .andExpect(jsonPath("$.data.nodes[0].nodeId").value(relatedConceptId))
                .andExpect(jsonPath("$.data.nodes[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.nodes[0].reasonSummary").value(org.hamcrest.Matchers.containsString("entry knowledge point")))
                .andExpect(jsonPath("$.data.nodes[1].nodeId").value(entryPointId))
                .andExpect(jsonPath("$.data.nodes[1].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.nodes[2].nodeId").value(advancedConceptId))
                .andExpect(jsonPath("$.data.nodes[2].status").value("ACTIVE"));
    }

    @Test
    void prioritizesLowMasteryPrerequisiteForRemediationBeforeOrdinaryReadyNodes() throws Exception {
        String courseId = createCourse();
        String chapterId = createChapter(courseId);
        String ordinaryEntryId = createKnowledgePoint(courseId, chapterId, "REST Controller Basics", 0.25);
        String isolatedLowId = createKnowledgePoint(courseId, chapterId, "SQL Syntax Drill", 0.20);
        String weakPrerequisiteId = createKnowledgePoint(courseId, chapterId, "Entity Mapping", 0.30);
        String downstreamId = createKnowledgePoint(courseId, chapterId, "Repository Services", 0.55);
        createDependency(downstreamId, weakPrerequisiteId);
        seedEnrollment(courseId, "learner_remediation", "ACTIVE");
        saveMastery("learner_remediation", ordinaryEntryId, 0.40);
        saveMastery("learner_remediation", isolatedLowId, 0.30);
        saveMastery("learner_remediation", weakPrerequisiteId, 0.50);

        mockMvc.perform(post("/api/learning-paths")
                        .header("X-User-Id", "learner_remediation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "learner_remediation",
                                  "goalId": "%s"
                                }
                                """.formatted(courseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.nodes.length()").value(4))
                .andExpect(jsonPath("$.data.nodes[0].nodeId").value(weakPrerequisiteId))
                .andExpect(jsonPath("$.data.nodes[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.nodes[0].mastery").value(0.50))
                .andExpect(jsonPath("$.data.nodes[0].reasonSummary").value(org.hamcrest.Matchers.containsString("remediation threshold 0.6")))
                .andExpect(jsonPath("$.data.nodes[0].reasonSummary").value(org.hamcrest.Matchers.containsString("downstream knowledge")))
                .andExpect(jsonPath("$.data.nodes[1].nodeId").value(ordinaryEntryId))
                .andExpect(jsonPath("$.data.nodes[1].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.nodes[2].nodeId").value(isolatedLowId))
                .andExpect(jsonPath("$.data.nodes[2].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.nodes[2].mastery").value(0.30))
                .andExpect(jsonPath("$.data.nodes[2].reasonSummary").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("remediation threshold"))))
                .andExpect(jsonPath("$.data.nodes[3].nodeId").value(downstreamId))
                .andExpect(jsonPath("$.data.nodes[3].status").value("LOCKED"))
                .andExpect(jsonPath("$.data.nodes[3].reasonSummary").value(org.hamcrest.Matchers.containsString("Entity Mapping")));
    }

    @Test
    void courseBoundLearningPathRequiresActiveEnrollmentButTemplateGoalsStayCompatible() throws Exception {
        String courseId = createCourse();
        String chapterId = createChapter(courseId);
        createKnowledgePoint(courseId, chapterId, "Enrollment Protected Knowledge", 0.35);

        mockMvc.perform(post("/api/learning-paths")
                        .header("X-User-Id", "learner_unenrolled")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "learner_unenrolled",
                                  "goalId": "%s"
                                }
                                """.formatted(courseId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());

        assertThat(learningPathRepository.findAll())
                .noneMatch(path -> "learner_unenrolled".equals(path.getLearnerId()));
        assertThat(learningPathNodeRepository.findAll())
                .noneMatch(node -> "learner_unenrolled".equals(node.getLearnerId()));
        assertThat(learningEventRepository.countByLearnerId("learner_unenrolled")).isZero();

        seedEnrollment(courseId, "learner_enrolled", "ACTIVE");
        mockMvc.perform(post("/api/learning-paths")
                        .header("X-User-Id", "learner_enrolled")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "learner_enrolled",
                                  "goalId": "%s"
                                }
                                """.formatted(courseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.goalId").value(courseId));

        mockMvc.perform(post("/api/learning-paths")
                        .header("X-User-Id", "learner_template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "learner_template",
                                  "goalId": "goal_spring_boot"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.goalId").value("goal_spring_boot"));
    }

    @Test
    void courseBoundLearningPathCreateUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader() throws Exception {
        String courseId = createCourse();

        mockMvc.perform(post("/api/learning-paths")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "alice",
                                  "goalId": "%s"
                                }
                                """.formatted(courseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.learnerId").value("alice"))
                .andExpect(jsonPath("$.data.goalId").value(courseId));

        assertThat(learningPathRepository.findAll())
                .anySatisfy(path -> {
                    assertThat(path.getLearnerId()).isEqualTo("alice");
                    assertThat(path.getGoalId()).isEqualTo(courseId);
                });
    }

    @Test
    void courseBoundLearningPathCreateRejectsBearerUserSubjectAdminRoleConfusionWithoutPersisting() throws Exception {
        String courseId = createCourse();

        String responseBody = mockMvc.perform(post("/api/learning-paths")
                        .header("Authorization", "Bearer " + jwt("admin", "Subject Admin", List.of("USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "alice",
                                  "goalId": "%s"
                                }
                                """.formatted(courseId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody).doesNotContain(courseId);
        assertThat(learningPathRepository.findAll())
                .noneMatch(path -> "alice".equals(path.getLearnerId()) && courseId.equals(path.getGoalId()));
        assertThat(learningPathNodeRepository.findAll())
                .noneMatch(node -> "alice".equals(node.getLearnerId()));
        assertThat(learningEventRepository.countByLearnerId("alice")).isZero();
    }

    private String createCourse() throws Exception {
        String body = mockMvc.perform(post("/api/courses")
                        .header("X-User-Id", "teacher_path")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Spring Boot DAG Course",
                                  "description": "Knowledge graph path planning.",
                                  "teacherId": "teacher_path"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).path("data").path("id").asText();
    }

    private String createChapter(String courseId) throws Exception {
        String body = mockMvc.perform(post("/api/courses/{courseId}/chapters", courseId)
                        .header("X-User-Id", "teacher_path")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Persistence Path",
                                  "sequenceNo": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).path("data").path("id").asText();
    }

    private String createKnowledgePoint(String courseId, String chapterId, String title, double difficulty) throws Exception {
        String body = mockMvc.perform(post("/api/knowledge-points")
                        .header("X-User-Id", "teacher_path")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseId": "%s",
                                  "chapterId": "%s",
                                  "title": "%s",
                                  "description": "Path node %s",
                                  "difficulty": %s
                                }
                                """.formatted(courseId, chapterId, title, title, difficulty)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).path("data").path("id").asText();
    }

    private void createDependency(String knowledgePointId, String prerequisiteId) throws Exception {
        createDependency(knowledgePointId, prerequisiteId, "PREREQUISITE");
    }

    private void createDependency(String knowledgePointId, String prerequisiteId, String dependencyType) throws Exception {
        mockMvc.perform(post("/api/knowledge-dependencies")
                        .header("X-User-Id", "teacher_path")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "knowledgePointId": "%s",
                                  "prerequisiteId": "%s",
                                  "dependencyType": "%s"
                                }
                                """.formatted(knowledgePointId, prerequisiteId, dependencyType)))
                .andExpect(status().isOk());
    }

    private void saveMastery(String learnerId, String knowledgePointId, double masteryValue) {
        MasteryRecord mastery = new MasteryRecord();
        mastery.setLearnerId(learnerId);
        mastery.setKnowledgePointId(knowledgePointId);
        mastery.setMastery(masteryValue);
        mastery.setSourceType("TEST_SEED");
        mastery.setSourceId("seed_mastery");
        mastery.setReasonSummary("Seeded mastery for DAG path planning.");
        mastery.setTraceId("trace_dag_seed");
        masteryRecordRepository.save(mastery);
    }

    private void seedEnrollment(String courseId, String learnerId, String status) {
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setCourseId(courseId);
        enrollment.setLearnerId(learnerId);
        enrollment.setStatus(status);
        courseEnrollmentRepository.save(enrollment);
    }

    private String createTemplatePath(String learnerId, String goalId) throws Exception {
        String body = mockMvc.perform(post("/api/learning-paths")
                        .header("X-User-Id", learnerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "%s",
                                  "goalId": "%s"
                                }
                                """.formatted(learnerId, goalId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).path("data").path("pathId").asText();
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

    private void postProfileUpdate(String learnerId, String sourceType, String message) throws Exception {
        mockMvc.perform(post("/api/profile/dialogue/extract")
                        .header("X-User-Id", learnerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "%s",
                                  "sourceType": "%s",
                                  "message": "%s"
                                }
                                """.formatted(learnerId, sourceType, message)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.profileDraft.learnerId").value(learnerId))
                .andExpect(jsonPath("$.data.traceId").isNotEmpty());
    }
}
