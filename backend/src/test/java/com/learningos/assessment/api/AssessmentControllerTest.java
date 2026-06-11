package com.learningos.assessment.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningos.knowledge.domain.Course;
import com.learningos.knowledge.domain.CourseEnrollment;
import com.learningos.knowledge.domain.KnowledgePoint;
import com.learningos.knowledge.repository.CourseEnrollmentRepository;
import com.learningos.knowledge.repository.CourseRepository;
import com.learningos.knowledge.repository.KnowledgePointRepository;
import com.learningos.learning.domain.LearningPath;
import com.learningos.learning.domain.LearningPathNode;
import com.learningos.learning.domain.MasteryRecord;
import com.learningos.assessment.repository.AnswerRecordRepository;
import com.learningos.assessment.repository.GradingResultRepository;
import com.learningos.assessment.repository.WrongQuestionRepository;
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
class AssessmentControllerTest {

    private static final String AUTH_SECRET = "unit-test-secret-32-bytes-long!!";
    private static final String AUTH_ISSUER = "learning-os";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final AnswerRecordRepository answerRecordRepository;
    private final GradingResultRepository gradingResultRepository;
    private final MasteryRecordRepository masteryRecordRepository;
    private final WrongQuestionRepository wrongQuestionRepository;
    private final LearningEventRepository learningEventRepository;
    private final LearningPathRepository learningPathRepository;
    private final LearningPathNodeRepository learningPathNodeRepository;
    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final KnowledgePointRepository knowledgePointRepository;

    AssessmentControllerTest(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            AnswerRecordRepository answerRecordRepository,
            GradingResultRepository gradingResultRepository,
            MasteryRecordRepository masteryRecordRepository,
            WrongQuestionRepository wrongQuestionRepository,
            LearningEventRepository learningEventRepository,
            LearningPathRepository learningPathRepository,
            LearningPathNodeRepository learningPathNodeRepository,
            CourseRepository courseRepository,
            CourseEnrollmentRepository courseEnrollmentRepository,
            KnowledgePointRepository knowledgePointRepository
    ) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.answerRecordRepository = answerRecordRepository;
        this.gradingResultRepository = gradingResultRepository;
        this.masteryRecordRepository = masteryRecordRepository;
        this.wrongQuestionRepository = wrongQuestionRepository;
        this.learningEventRepository = learningEventRepository;
        this.learningPathRepository = learningPathRepository;
        this.learningPathNodeRepository = learningPathNodeRepository;
        this.courseRepository = courseRepository;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
        this.knowledgePointRepository = knowledgePointRepository;
    }

    @Test
    void submitAnswerGradesUpdatesMasteryAndTriggersReplanRecord() throws Exception {
        LearningPath path = new LearningPath();
        path.setId("path_alice_sql");
        path.setLearnerId("alice");
        path.setGoalId("goal_sql");
        path.setStatus("ACTIVE");
        path.setReasonSummary("Original path reason.");
        path.setTraceId("trc_path");
        learningPathRepository.save(path);

        LearningPathNode node = new LearningPathNode();
        node.setId("node_alice_sql_join");
        node.setPathId(path.getId());
        node.setLearnerId("alice");
        node.setKnowledgePointId("kp_sql_join");
        node.setTitle("SQL JOIN diagnosis");
        node.setStatus("DONE");
        node.setMastery(0.86);
        node.setReasonSummary("Original node reason.");
        node.setSequenceNo(1);
        learningPathNodeRepository.save(node);

        MasteryRecord previousMastery = new MasteryRecord();
        previousMastery.setLearnerId("alice");
        previousMastery.setKnowledgePointId("kp_sql_join");
        previousMastery.setMastery(0.86);
        previousMastery.setSourceType("PREVIOUS_ASSESSMENT");
        previousMastery.setSourceId("seed_previous_mastery");
        previousMastery.setReasonSummary("Seed mastery before current answer.");
        previousMastery.setTraceId("trc_previous_mastery");
        masteryRecordRepository.save(previousMastery);

        String responseJson = mockMvc.perform(post("/api/assessment/answers")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                  "learnerId": "alice",
                  "questionId": "q_sql_join",
                  "answer": "JOIN duplicates happen when a one-to-many relation is joined.",
                  "requestId": "req_answer_replan_once"
                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answerId").isNotEmpty())
                .andExpect(jsonPath("$.data.gradingResultId").isNotEmpty())
                .andExpect(jsonPath("$.data.score").value(0.85))
                .andExpect(jsonPath("$.data.masteryUpdates.length()").value(1))
                .andExpect(jsonPath("$.data.masteryUpdates[0].knowledgePointId").value("kp_sql_join"))
                .andExpect(jsonPath("$.data.masteryUpdates[0].beforeMastery").value(0.86))
                .andExpect(jsonPath("$.data.masteryUpdates[0].afterMastery").value(0.78))
                .andExpect(jsonPath("$.data.masteryUpdates[0].reasonSummary").isNotEmpty())
                .andExpect(jsonPath("$.data.feedbackId").isNotEmpty())
                .andExpect(jsonPath("$.data.replanTriggered").value(true))
                .andExpect(jsonPath("$.data.replanRecordId").isNotEmpty())
                .andExpect(jsonPath("$.data.replanDecision.status").value("REPLAN_REQUIRED"))
                .andExpect(jsonPath("$.data.replanDecision.replanRequired").value(true))
                .andExpect(jsonPath("$.data.replanDecision.affectedPathIds[0]").value("path_alice_sql"))
                .andExpect(jsonPath("$.data.replanDecision.reasonSummary").isNotEmpty())
                .andExpect(jsonPath("$.data.replanDecision.traceId").isNotEmpty())
                .andExpect(jsonPath("$.data.wrongCauseAnalysis").isNotEmpty())
                .andExpect(jsonPath("$.data.feedbackDiagnosis.wrongCauseCategory").value("TRANSFER_WEAKNESS"))
                .andExpect(jsonPath("$.data.feedbackDiagnosis.misconception").isNotEmpty())
                .andExpect(jsonPath("$.data.feedbackDiagnosis.missingPrerequisite").isNotEmpty())
                .andExpect(jsonPath("$.data.feedbackDiagnosis.recommendedRemediation").isNotEmpty())
                .andExpect(jsonPath("$.data.feedbackDiagnosis.evidence").isNotEmpty())
                .andExpect(jsonPath("$.data.resourcePushStrategy").value("PUSH_REMEDIAL_PRACTICE_AND_CODE_LAB"))
                .andExpect(jsonPath("$.data.traceId").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(answerRecordRepository.countByLearnerId("alice")).isEqualTo(1);
        assertThat(gradingResultRepository.count()).isEqualTo(1);
        assertThat(masteryRecordRepository.findFirstByLearnerIdAndKnowledgePointIdOrderByUpdatedAtDesc("alice", "kp_sql_join"))
                .hasValueSatisfying(record -> assertThat(record.getMastery()).isEqualTo(0.78));
        assertThat(wrongQuestionRepository.countByLearnerId("alice")).isEqualTo(1);
        assertThat(learningEventRepository.countByLearnerId("alice")).isEqualTo(1);

        JsonNode response = objectMapper.readTree(responseJson);
        String responseTraceId = response.path("data").path("traceId").asText();
        String responseReplanRecordId = response.path("data").path("replanRecordId").asText();
        assertThat(response.path("data").path("replanDecision").path("traceId").asText())
                .isEqualTo(responseTraceId);

        var wrongQuestion = wrongQuestionRepository.findAll().getFirst();
        assertThat(wrongQuestion.getReplanRecordId()).isEqualTo(responseReplanRecordId);
        assertThat(wrongQuestion.getReplanRecordId()).isEqualTo(responseTraceId);

        String causeAnalysisJson = wrongQuestion.getCauseAnalysis();
        JsonNode causeAnalysis = objectMapper.readTree(causeAnalysisJson);
        assertThat(causeAnalysis.path("wrongCauseCategory").asText()).isEqualTo("TRANSFER_WEAKNESS");
        assertThat(causeAnalysis.path("misconception").asText()).isNotBlank();
        assertThat(causeAnalysis.path("recommendedRemediation").asText()).isNotBlank();

        String eventPayloadJson = learningEventRepository.findAll().getFirst().getPayloadJson();
        JsonNode eventPayload = objectMapper.readTree(eventPayloadJson);
        assertThat(eventPayload.path("feedbackDiagnosis").path("wrongCauseCategory").asText())
                .isEqualTo("TRANSFER_WEAKNESS");
        assertThat(eventPayload.path("replanDecision").path("status").asText())
                .isEqualTo("REPLAN_REQUIRED");
        assertThat(learningPathNodeRepository.findById("node_alice_sql_join"))
                .hasValueSatisfying(updatedNode -> {
                    assertThat(updatedNode.getMastery()).isEqualTo(0.78);
                    assertThat(updatedNode.getStatus()).isEqualTo("REPLAN_REQUIRED");
                    assertThat(updatedNode.getReasonSummary()).contains("learning path requires replanning");
                });
        assertThat(learningPathRepository.findById("path_alice_sql"))
                .hasValueSatisfying(updatedPath -> {
                    assertThat(updatedPath.getStatus()).isEqualTo("REPLAN_REQUIRED");
                    assertThat(updatedPath.getTraceId()).isEqualTo(responseTraceId);
                });
    }

    @Test
    void submitAnswerReturnsNoReplanRequiredDecisionWhenLearnerHasNoPathNodes() throws Exception {
        String responseJson = mockMvc.perform(post("/api/assessment/answers")
                        .header("X-User-Id", "no_path_learner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                  "learnerId": "no_path_learner",
                  "questionId": "q_sql_join",
                  "answer": "JOIN duplicates happen when a one-to-many relation is joined.",
                  "requestId": "req_answer_no_path_once"
                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.masteryUpdates.length()").value(1))
                .andExpect(jsonPath("$.data.masteryUpdates[0].knowledgePointId").value("kp_sql_join"))
                .andExpect(jsonPath("$.data.masteryUpdates[0].beforeMastery").value(0.42))
                .andExpect(jsonPath("$.data.masteryUpdates[0].afterMastery").value(0.58))
                .andExpect(jsonPath("$.data.replanRecordId").isNotEmpty())
                .andExpect(jsonPath("$.data.replanDecision.status").value("NO_REPLAN_REQUIRED"))
                .andExpect(jsonPath("$.data.replanDecision.replanRequired").value(false))
                .andExpect(jsonPath("$.data.replanDecision.affectedPathIds.length()").value(0))
                .andExpect(jsonPath("$.data.replanDecision.reasonSummary").value(
                        "No active learning path nodes reference knowledge point kp_sql_join."))
                .andExpect(jsonPath("$.data.replanDecision.traceId").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseJson);
        String responseTraceId = response.path("data").path("traceId").asText();
        assertThat(response.path("data").path("replanRecordId").asText()).isEqualTo(responseTraceId);
        assertThat(response.path("data").path("replanDecision").path("traceId").asText())
                .isEqualTo(responseTraceId);
        assertThat(masteryRecordRepository.findFirstByLearnerIdAndKnowledgePointIdOrderByUpdatedAtDesc(
                "no_path_learner",
                "kp_sql_join"
        )).hasValueSatisfying(record -> {
            assertThat(record.getMastery()).isEqualTo(0.58);
            assertThat(record.getReasonSummary()).contains("0.42 -> 0.58");
        });
        assertThat(wrongQuestionRepository.findAll().getFirst()).satisfies(wrongQuestion -> {
            assertThat(wrongQuestion.getReplanRecordId()).isEqualTo(responseTraceId);
            assertThat(wrongQuestion.getKnowledgePointId()).isEqualTo("kp_sql_join");
        });
    }

    @Test
    void rejectsCrossUserAnswerSubmission() throws Exception {
        mockMvc.perform(post("/api/assessment/answers")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                  "learnerId": "bob",
                  "questionId": "q_sql_join",
                  "answer": "JOIN duplicates happen when a one-to-many relation is joined.",
                  "requestId": "req_answer_cross_user"
                }
                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotDistinguishForeignAnswerFromMissingAnswer() throws Exception {
        String bobAnswerId = submitAnswer("bob", "q_sql_join", "req_bob_answer_for_idor");

        String foreignBody = mockMvc.perform(get("/api/assessment/answers/{answerId}", bobAnswerId)
                        .header("X-User-Id", "alice"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String missingBody = mockMvc.perform(get("/api/assessment/answers/{answerId}", "ans_missing_for_idor")
                        .header("X-User-Id", "alice"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(foreignBody)
                .doesNotContain(bobAnswerId)
                .doesNotContain("bob")
                .doesNotContain("q_sql_join");
        assertThat(missingBody).doesNotContain("ans_missing_for_idor");
    }

    @Test
    void teacherCanReadOwnCourseEnrolledLearnerAnswerButNotForeignCourseAnswer() throws Exception {
        String aliceQuestionId = seedCourseKnowledgeAndEnrollment(
                "course_teacher_a_sql",
                "teacher_a",
                "alice",
                "SQL JOIN"
        );
        String bobQuestionId = seedCourseKnowledgeAndEnrollment(
                "course_teacher_b_sql",
                "teacher_b",
                "bob",
                "SQL window"
        );
        String aliceAnswerId = submitAnswer("alice", aliceQuestionId, "req_alice_course_answer");
        String bobAnswerId = submitAnswer("bob", bobQuestionId, "req_bob_course_answer");

        mockMvc.perform(get("/api/assessment/answers/{answerId}", aliceAnswerId)
                        .header("X-User-Id", "teacher_a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.answerId").value(aliceAnswerId))
                .andExpect(jsonPath("$.data.learnerId").value("alice"))
                .andExpect(jsonPath("$.data.questionId").value(aliceQuestionId))
                .andExpect(jsonPath("$.data.score").value(0.85))
                .andExpect(jsonPath("$.data.requestId").doesNotExist())
                .andExpect(jsonPath("$.data.requestHash").doesNotExist())
                .andExpect(jsonPath("$.data.responseJson").doesNotExist());

        String body = mockMvc.perform(get("/api/assessment/answers/{answerId}", bobAnswerId)
                        .header("X-User-Id", "teacher_a"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .doesNotContain(bobAnswerId)
                .doesNotContain("bob")
                .doesNotContain("course_teacher_b_sql");
    }

    @Test
    void adminCanReadAnyAnswerAndReceivesNotFoundForMissingAnswer() throws Exception {
        String answerId = submitAnswer("bob", "q_sql_join", "req_admin_answer_read");

        mockMvc.perform(get("/api/assessment/answers/{answerId}", answerId)
                        .header("X-User-Id", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answerId").value(answerId))
                .andExpect(jsonPath("$.data.learnerId").value("bob"))
                .andExpect(jsonPath("$.data.requestHash").doesNotExist())
                .andExpect(jsonPath("$.data.responseJson").doesNotExist());

        mockMvc.perform(get("/api/assessment/answers/{answerId}", "ans_missing_admin")
                        .header("X-User-Id", "admin"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void answerDetailUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader() throws Exception {
        String answerId = submitAnswer("bob", "q_sql_join", "req_bearer_admin_answer_read");

        mockMvc.perform(get("/api/assessment/answers/{answerId}", answerId)
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.answerId").value(answerId))
                .andExpect(jsonPath("$.data.learnerId").value("bob"));
    }

    @Test
    void answerDetailAllowsBearerStudentRoleAndIgnoresSpoofedUserIdHeader() throws Exception {
        String answerId = submitAnswer("alice", "q_sql_join", "req_bearer_student_answer_detail");

        mockMvc.perform(get("/api/assessment/answers/{answerId}", answerId)
                        .header("Authorization", "Bearer " + jwt("alice", "Alice", List.of("USER")))
                        .header("X-User-Id", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.answerId").value(answerId))
                .andExpect(jsonPath("$.data.learnerId").value("alice"))
                .andExpect(jsonPath("$.data.requestId").doesNotExist())
                .andExpect(jsonPath("$.data.requestHash").doesNotExist())
                .andExpect(jsonPath("$.data.responseJson").doesNotExist())
                .andExpect(jsonPath("$.data.payloadJson").doesNotExist());
    }

    @Test
    void answerDetailRejectsBearerUserSubjectAdminRoleConfusion() throws Exception {
        String answerId = submitAnswer("bob", "q_sql_join", "req_user_subject_admin_answer_read");

        mockMvc.perform(get("/api/assessment/answers/{answerId}", answerId)
                        .header("Authorization", "Bearer " + jwt("admin", "Subject Admin", List.of("USER")))
                        .header("X-User-Id", "admin"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void answerDetailRejectsBearerUserSubjectTeacherPrefixRoleConfusion() throws Exception {
        String questionId = seedCourseKnowledgeAndEnrollment(
                "course_user_subject_teacher_answer_detail",
                "teacher_1",
                "alice",
                "User subject teacher answer detail"
        );
        String answerId = submitAnswer("alice", questionId, "req_user_subject_teacher_answer_detail");

        String body = mockMvc.perform(get("/api/assessment/answers/{answerId}", answerId)
                        .header("Authorization", "Bearer " + jwt("teacher_1", "Subject Teacher", List.of("USER")))
                        .header("X-User-Id", "teacher_1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain(answerId);
    }

    @Test
    void teacherDetailRejectsInactiveEnrollmentEvenForOwnCourseAssessmentRecords() throws Exception {
        String questionId = seedCourseKnowledgeAndEnrollment(
                "course_teacher_inactive_detail",
                "teacher_a",
                "alice",
                "Teacher inactive enrollment detail"
        );
        String answerId = submitAnswer("alice", questionId, "req_teacher_inactive_detail");
        String wrongQuestionId = wrongQuestionIdForAnswer(answerId);
        courseEnrollmentRepository.findByCourseIdAndLearnerId("course_teacher_inactive_detail", "alice")
                .orElseThrow()
                .setStatus("DROPPED");

        String answerBody = mockMvc.perform(get("/api/assessment/answers/{answerId}", answerId)
                        .header("Authorization", "Bearer " + jwt("teacher_a", "Teacher A", List.of("TEACHER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(answerBody)
                .doesNotContain(answerId)
                .doesNotContain("alice");

        String wrongQuestionBody = mockMvc.perform(get("/api/assessment/wrong-questions/{wrongQuestionId}", wrongQuestionId)
                        .header("Authorization", "Bearer " + jwt("teacher_a", "Teacher A", List.of("TEACHER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(wrongQuestionBody)
                .doesNotContain(wrongQuestionId)
                .doesNotContain(answerId)
                .doesNotContain("alice");
    }

    @Test
    void wrongQuestionDetailReusesAssessmentRecordScope() throws Exception {
        String questionId = seedCourseKnowledgeAndEnrollment(
                "course_teacher_a_wrong",
                "teacher_a",
                "alice",
                "SQL JOIN wrong"
        );
        String aliceAnswerId = submitAnswer("alice", questionId, "req_alice_wrong_question");
        String wrongQuestionId = wrongQuestionRepository.findAll().getFirst().getId();
        String knowledgePointId = wrongQuestionRepository.findAll().getFirst().getKnowledgePointId();

        mockMvc.perform(get("/api/assessment/wrong-questions/{wrongQuestionId}", wrongQuestionId)
                        .header("X-User-Id", "teacher_a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.wrongQuestionId").value(wrongQuestionId))
                .andExpect(jsonPath("$.data.answerId").value(aliceAnswerId))
                .andExpect(jsonPath("$.data.learnerId").value("alice"))
                .andExpect(jsonPath("$.data.knowledgePointId").value(knowledgePointId))
                .andExpect(jsonPath("$.data.resourcePushStrategy").value("PUSH_REMEDIAL_PRACTICE_AND_CODE_LAB"))
                .andExpect(jsonPath("$.data.responseJson").doesNotExist())
                .andExpect(jsonPath("$.data.payloadJson").doesNotExist());

        mockMvc.perform(get("/api/assessment/wrong-questions/{wrongQuestionId}", wrongQuestionId)
                        .header("X-User-Id", "bob"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/api/assessment/wrong-questions/{wrongQuestionId}", "wq_missing_for_idor")
                        .header("X-User-Id", "bob"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void wrongQuestionDetailAllowsBearerTeacherWithoutSubjectPrefixForOwnCourse() throws Exception {
        String questionId = seedCourseKnowledgeAndEnrollment(
                "course_bearer_instructor_wrong_detail",
                "instructor_1",
                "alice",
                "Bearer instructor wrong detail"
        );
        String answerId = submitAnswer("alice", questionId, "req_bearer_instructor_wrong_detail");
        String wrongQuestionId = wrongQuestionRepository
                .findFirstByAnswerIdOrderByCreatedAtDesc(answerId)
                .orElseThrow()
                .getId();

        mockMvc.perform(get("/api/assessment/wrong-questions/{wrongQuestionId}", wrongQuestionId)
                        .header("Authorization", "Bearer " + jwt("instructor_1", "Instructor", List.of("TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.wrongQuestionId").value(wrongQuestionId))
                .andExpect(jsonPath("$.data.learnerId").value("alice"));
    }

    @Test
    void wrongQuestionDetailUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader() throws Exception {
        String answerId = submitAnswer("bob", "q_sql_join", "req_bearer_admin_wrong_detail");
        String wrongQuestionId = wrongQuestionIdForAnswer(answerId);

        mockMvc.perform(get("/api/assessment/wrong-questions/{wrongQuestionId}", wrongQuestionId)
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.wrongQuestionId").value(wrongQuestionId))
                .andExpect(jsonPath("$.data.answerId").value(answerId))
                .andExpect(jsonPath("$.data.learnerId").value("bob"))
                .andExpect(jsonPath("$.data.causeAnalysis").doesNotExist())
                .andExpect(jsonPath("$.data.payloadJson").doesNotExist())
                .andExpect(jsonPath("$.data.responseJson").doesNotExist());
    }

    @Test
    void wrongQuestionDetailAllowsBearerStudentRoleAndIgnoresSpoofedUserIdHeader() throws Exception {
        String answerId = submitAnswer("alice", "q_sql_join", "req_bearer_student_wrong_detail");
        String wrongQuestionId = wrongQuestionIdForAnswer(answerId);

        mockMvc.perform(get("/api/assessment/wrong-questions/{wrongQuestionId}", wrongQuestionId)
                        .header("Authorization", "Bearer " + jwt("alice", "Alice", List.of("USER")))
                        .header("X-User-Id", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.wrongQuestionId").value(wrongQuestionId))
                .andExpect(jsonPath("$.data.answerId").value(answerId))
                .andExpect(jsonPath("$.data.learnerId").value("alice"))
                .andExpect(jsonPath("$.data.causeAnalysis").doesNotExist())
                .andExpect(jsonPath("$.data.payloadJson").doesNotExist())
                .andExpect(jsonPath("$.data.responseJson").doesNotExist());
    }

    @Test
    void wrongQuestionDetailRejectsBearerUserSubjectAdminRoleConfusion() throws Exception {
        String answerId = submitAnswer("bob", "q_sql_join", "req_user_subject_admin_wrong_detail");
        String wrongQuestionId = wrongQuestionIdForAnswer(answerId);

        String body = mockMvc.perform(get("/api/assessment/wrong-questions/{wrongQuestionId}", wrongQuestionId)
                        .header("Authorization", "Bearer " + jwt("admin", "Subject Admin", List.of("USER")))
                        .header("X-User-Id", "admin"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .doesNotContain(wrongQuestionId)
                .doesNotContain(answerId);
    }

    @Test
    void wrongQuestionDetailRejectsBearerUserSubjectTeacherPrefixRoleConfusion() throws Exception {
        String questionId = seedCourseKnowledgeAndEnrollment(
                "course_user_subject_teacher_wrong_detail",
                "teacher_1",
                "alice",
                "User subject teacher wrong detail"
        );
        String answerId = submitAnswer("alice", questionId, "req_user_subject_teacher_wrong_detail");
        String wrongQuestionId = wrongQuestionRepository
                .findFirstByAnswerIdOrderByCreatedAtDesc(answerId)
                .orElseThrow()
                .getId();

        mockMvc.perform(get("/api/assessment/wrong-questions/{wrongQuestionId}", wrongQuestionId)
                        .header("Authorization", "Bearer " + jwt("teacher_1", "Subject Teacher", List.of("USER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void answerListUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader() throws Exception {
        String aliceAnswerId = submitAnswer("alice", "q_sql_join", "req_bearer_admin_answer_list_alice");
        String bobAnswerId = submitAnswer("bob", "q_sql_join", "req_bearer_admin_answer_list_bob");

        String body = mockMvc.perform(get("/api/assessment/answers")
                        .param("learnerId", "bob")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].answerId").value(bobAnswerId))
                .andExpect(jsonPath("$.data.items[0].learnerId").value("bob"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .contains(bobAnswerId)
                .doesNotContain(aliceAnswerId)
                .doesNotContain("\"requestId\"")
                .doesNotContain("\"requestHash\"")
                .doesNotContain("\"responseJson\"")
                .doesNotContain("\"payloadJson\"");
    }

    @Test
    void answerListAllowsBearerStudentRoleAndIgnoresSpoofedUserIdHeader() throws Exception {
        String aliceAnswerId = submitAnswer("alice", "q_sql_join", "req_bearer_student_answer_list_alice");
        String bobAnswerId = submitAnswer("bob", "q_sql_join", "req_bearer_student_answer_list_bob");

        String body = mockMvc.perform(get("/api/assessment/answers")
                        .header("Authorization", "Bearer " + jwt("alice", "Alice", List.of("USER")))
                        .header("X-User-Id", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].answerId").value(aliceAnswerId))
                .andExpect(jsonPath("$.data.items[0].learnerId").value("alice"))
                .andExpect(jsonPath("$.data.items[0].answer").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].requestId").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].requestHash").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].responseJson").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].payloadJson").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .contains(aliceAnswerId)
                .doesNotContain(bobAnswerId)
                .doesNotContain("\"answer\":")
                .doesNotContain("\"requestId\"")
                .doesNotContain("\"requestHash\"")
                .doesNotContain("\"responseJson\"")
                .doesNotContain("\"payloadJson\"");
    }

    @Test
    void studentListsOnlyOwnAnswerSummariesAndCannotQueryForeignLearner() throws Exception {
        String aliceAnswerId = submitAnswer("alice", "q_sql_join", "req_alice_answer_list_owner");
        String bobAnswerId = submitAnswer("bob", "q_sql_join", "req_bob_answer_list_owner");

        String body = mockMvc.perform(get("/api/assessment/answers")
                        .header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].answerId").value(aliceAnswerId))
                .andExpect(jsonPath("$.data.items[0].learnerId").value("alice"))
                .andExpect(jsonPath("$.data.items[0].questionId").value("q_sql_join"))
                .andExpect(jsonPath("$.data.items[0].score").value(0.85))
                .andExpect(jsonPath("$.data.items[0].safetyStatus").isNotEmpty())
                .andExpect(jsonPath("$.data.items[0].wrongQuestionId").isNotEmpty())
                .andExpect(jsonPath("$.data.items[0].answer").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].requestId").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].requestHash").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].responseJson").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].payloadJson").doesNotExist())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .doesNotContain(bobAnswerId)
                .doesNotContain("\"answer\":")
                .doesNotContain("\"requestId\"")
                .doesNotContain("\"requestHash\"")
                .doesNotContain("\"responseJson\"")
                .doesNotContain("\"payloadJson\"");

        String foreignBody = mockMvc.perform(get("/api/assessment/answers")
                        .param("learnerId", "bob")
                        .header("X-User-Id", "alice"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(foreignBody)
                .doesNotContain(bobAnswerId)
                .doesNotContain("q_sql_join");
    }

    @Test
    void studentListsOnlyOwnWrongQuestionSummariesAndCannotQueryForeignLearner() throws Exception {
        String aliceAnswerId = submitAnswer("alice", "q_sql_join", "req_alice_wrong_question_list_owner");
        String bobAnswerId = submitAnswer("bob", "q_sql_join", "req_bob_wrong_question_list_owner");
        String aliceWrongQuestionId = wrongQuestionRepository
                .findFirstByAnswerIdOrderByCreatedAtDesc(aliceAnswerId)
                .orElseThrow()
                .getId();
        String bobWrongQuestionId = wrongQuestionRepository
                .findFirstByAnswerIdOrderByCreatedAtDesc(bobAnswerId)
                .orElseThrow()
                .getId();

        String body = mockMvc.perform(get("/api/assessment/wrong-questions")
                        .header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].wrongQuestionId").value(aliceWrongQuestionId))
                .andExpect(jsonPath("$.data.items[0].answerId").value(aliceAnswerId))
                .andExpect(jsonPath("$.data.items[0].learnerId").value("alice"))
                .andExpect(jsonPath("$.data.items[0].questionId").value("q_sql_join"))
                .andExpect(jsonPath("$.data.items[0].knowledgePointId").value("kp_sql_join"))
                .andExpect(jsonPath("$.data.items[0].score").value(0.85))
                .andExpect(jsonPath("$.data.items[0].resourcePushStrategy")
                        .value("PUSH_REMEDIAL_PRACTICE_AND_CODE_LAB"))
                .andExpect(jsonPath("$.data.items[0].causeAnalysis").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].payloadJson").doesNotExist())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .doesNotContain(bobWrongQuestionId)
                .doesNotContain("\"causeAnalysis\"")
                .doesNotContain("\"payloadJson\"")
                .doesNotContain("\"responseJson\"");

        String foreignBody = mockMvc.perform(get("/api/assessment/wrong-questions")
                        .param("learnerId", "bob")
                        .header("X-User-Id", "alice"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(foreignBody)
                .doesNotContain(bobWrongQuestionId)
                .doesNotContain(bobAnswerId);
    }

    @Test
    void teacherListsOnlyOwnCourseActiveLearnerAnswersAndRequiresCourseScope() throws Exception {
        String aliceQuestionId = seedCourseKnowledgeAndEnrollment(
                "course_teacher_a_answer_list",
                "teacher_a",
                "alice",
                "SQL JOIN answer list"
        );
        String bobQuestionId = seedCourseKnowledgeAndEnrollment(
                "course_teacher_b_answer_list",
                "teacher_b",
                "bob",
                "SQL window answer list"
        );
        String aliceAnswerId = submitAnswer("alice", aliceQuestionId, "req_alice_teacher_answer_list");
        String bobAnswerId = submitAnswer("bob", bobQuestionId, "req_bob_teacher_answer_list");

        String body = mockMvc.perform(get("/api/assessment/answers")
                        .param("courseId", "course_teacher_a_answer_list")
                        .header("X-User-Id", "teacher_a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].answerId").value(aliceAnswerId))
                .andExpect(jsonPath("$.data.items[0].learnerId").value("alice"))
                .andExpect(jsonPath("$.data.items[0].questionId").value(aliceQuestionId))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain(bobAnswerId);

        mockMvc.perform(get("/api/assessment/answers")
                        .header("X-User-Id", "teacher_a"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data").doesNotExist());

        String foreignCourseBody = mockMvc.perform(get("/api/assessment/answers")
                        .param("courseId", "course_teacher_b_answer_list")
                        .header("X-User-Id", "teacher_a"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(foreignCourseBody)
                .doesNotContain(bobAnswerId)
                .doesNotContain("course_teacher_b_answer_list");

        String unenrolledLearnerBody = mockMvc.perform(get("/api/assessment/answers")
                        .param("courseId", "course_teacher_a_answer_list")
                        .param("learnerId", "bob")
                        .header("X-User-Id", "teacher_a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.items.length()").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(unenrolledLearnerBody).doesNotContain(bobAnswerId);
    }

    @Test
    void answerListAllowsBearerTeacherWithoutSubjectPrefixForOwnCourse() throws Exception {
        String questionId = seedCourseKnowledgeAndEnrollment(
                "course_bearer_instructor_answer_list",
                "instructor_1",
                "alice",
                "Bearer instructor answer list"
        );
        String answerId = submitAnswer("alice", questionId, "req_bearer_instructor_answer_list");

        mockMvc.perform(get("/api/assessment/answers")
                        .param("courseId", "course_bearer_instructor_answer_list")
                        .header("Authorization", "Bearer " + jwt("instructor_1", "Instructor", List.of("TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].answerId").value(answerId))
                .andExpect(jsonPath("$.data.items[0].learnerId").value("alice"));
    }

    @Test
    void answerListRejectsBearerUserSubjectAdminRoleConfusion() throws Exception {
        String answerId = submitAnswer("bob", "q_sql_join", "req_user_subject_admin_answer_list");

        String body = mockMvc.perform(get("/api/assessment/answers")
                        .param("learnerId", "bob")
                        .header("Authorization", "Bearer " + jwt("admin", "Subject Admin", List.of("USER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain(answerId);
    }

    @Test
    void teacherListsOnlyOwnCourseActiveLearnerWrongQuestionsAndRequiresCourseScope() throws Exception {
        String aliceQuestionId = seedCourseKnowledgeAndEnrollment(
                "course_teacher_a_wrong_list",
                "teacher_a",
                "alice",
                "SQL JOIN wrong list"
        );
        String bobQuestionId = seedCourseKnowledgeAndEnrollment(
                "course_teacher_b_wrong_list",
                "teacher_b",
                "bob",
                "SQL window wrong list"
        );
        String aliceAnswerId = submitAnswer("alice", aliceQuestionId, "req_alice_teacher_wrong_list");
        String bobAnswerId = submitAnswer("bob", bobQuestionId, "req_bob_teacher_wrong_list");
        String aliceWrongQuestionId = wrongQuestionRepository
                .findFirstByAnswerIdOrderByCreatedAtDesc(aliceAnswerId)
                .orElseThrow()
                .getId();
        String bobWrongQuestionId = wrongQuestionRepository
                .findFirstByAnswerIdOrderByCreatedAtDesc(bobAnswerId)
                .orElseThrow()
                .getId();

        String body = mockMvc.perform(get("/api/assessment/wrong-questions")
                        .param("courseId", "course_teacher_a_wrong_list")
                        .header("X-User-Id", "teacher_a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].wrongQuestionId").value(aliceWrongQuestionId))
                .andExpect(jsonPath("$.data.items[0].answerId").value(aliceAnswerId))
                .andExpect(jsonPath("$.data.items[0].learnerId").value("alice"))
                .andExpect(jsonPath("$.data.items[0].questionId").value(aliceQuestionId))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain(bobWrongQuestionId);

        mockMvc.perform(get("/api/assessment/wrong-questions")
                        .header("X-User-Id", "teacher_a"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data").doesNotExist());

        String foreignCourseBody = mockMvc.perform(get("/api/assessment/wrong-questions")
                        .param("courseId", "course_teacher_b_wrong_list")
                        .header("X-User-Id", "teacher_a"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(foreignCourseBody)
                .doesNotContain(bobWrongQuestionId)
                .doesNotContain("course_teacher_b_wrong_list");

        String unenrolledLearnerBody = mockMvc.perform(get("/api/assessment/wrong-questions")
                        .param("courseId", "course_teacher_a_wrong_list")
                        .param("learnerId", "bob")
                        .header("X-User-Id", "teacher_a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.items.length()").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(unenrolledLearnerBody).doesNotContain(bobWrongQuestionId);
    }

    @Test
    void wrongQuestionListAllowsBearerTeacherWithoutSubjectPrefixForOwnCourse() throws Exception {
        String aliceQuestionId = seedCourseKnowledgeAndEnrollment(
                "course_bearer_instructor_wrong_list",
                "instructor_1",
                "alice",
                "Bearer instructor wrong list"
        );
        String bobQuestionId = seedCourseKnowledgeAndEnrollment(
                "course_bearer_other_wrong_list",
                "other_instructor",
                "bob",
                "Bearer other wrong list"
        );
        String aliceAnswerId = submitAnswer("alice", aliceQuestionId, "req_bearer_instructor_wrong_list");
        String bobAnswerId = submitAnswer("bob", bobQuestionId, "req_bearer_other_wrong_list");
        String aliceWrongQuestionId = wrongQuestionIdForAnswer(aliceAnswerId);
        String bobWrongQuestionId = wrongQuestionIdForAnswer(bobAnswerId);

        String body = mockMvc.perform(get("/api/assessment/wrong-questions")
                        .param("courseId", "course_bearer_instructor_wrong_list")
                        .header("Authorization", "Bearer " + jwt("instructor_1", "Instructor", List.of("TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].wrongQuestionId").value(aliceWrongQuestionId))
                .andExpect(jsonPath("$.data.items[0].answerId").value(aliceAnswerId))
                .andExpect(jsonPath("$.data.items[0].learnerId").value("alice"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .contains(aliceWrongQuestionId)
                .doesNotContain(bobWrongQuestionId)
                .doesNotContain(bobAnswerId)
                .doesNotContain("\"causeAnalysis\"")
                .doesNotContain("\"payloadJson\"")
                .doesNotContain("\"responseJson\"");
    }

    @Test
    void wrongQuestionListRejectsBearerUserSubjectAdminRoleConfusion() throws Exception {
        String answerId = submitAnswer("bob", "q_sql_join", "req_user_subject_admin_wrong_list");
        String wrongQuestionId = wrongQuestionIdForAnswer(answerId);

        String body = mockMvc.perform(get("/api/assessment/wrong-questions")
                        .param("learnerId", "bob")
                        .header("Authorization", "Bearer " + jwt("admin", "Subject Admin", List.of("USER")))
                        .header("X-User-Id", "admin"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .doesNotContain(wrongQuestionId)
                .doesNotContain(answerId);
    }

    @Test
    void wrongQuestionListRejectsBearerUserSubjectTeacherPrefixRoleConfusion() throws Exception {
        String questionId = seedCourseKnowledgeAndEnrollment(
                "course_user_subject_teacher_wrong_list",
                "teacher_1",
                "alice",
                "User subject teacher wrong list"
        );
        String answerId = submitAnswer("alice", questionId, "req_user_subject_teacher_wrong_list");
        String wrongQuestionId = wrongQuestionIdForAnswer(answerId);

        String body = mockMvc.perform(get("/api/assessment/wrong-questions")
                        .param("courseId", "course_user_subject_teacher_wrong_list")
                        .header("Authorization", "Bearer " + jwt("teacher_1", "Subject Teacher", List.of("USER")))
                        .header("X-User-Id", "teacher_1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .doesNotContain(wrongQuestionId)
                .doesNotContain(answerId);
    }

    @Test
    void adminListsAndFiltersAssessmentRecordsWithCourseNotFoundSemantics() throws Exception {
        String aliceQuestionId = seedCourseKnowledgeAndEnrollment(
                "course_admin_answer_list_a",
                "teacher_a",
                "alice",
                "SQL JOIN admin answer list"
        );
        String bobQuestionId = seedCourseKnowledgeAndEnrollment(
                "course_admin_answer_list_b",
                "teacher_b",
                "bob",
                "SQL window admin answer list"
        );
        String aliceAnswerId = submitAnswer("alice", aliceQuestionId, "req_alice_admin_answer_list");
        String bobAnswerId = submitAnswer("bob", bobQuestionId, "req_bob_admin_answer_list");
        String aliceWrongQuestionId = wrongQuestionRepository
                .findFirstByAnswerIdOrderByCreatedAtDesc(aliceAnswerId)
                .orElseThrow()
                .getId();
        String bobWrongQuestionId = wrongQuestionRepository
                .findFirstByAnswerIdOrderByCreatedAtDesc(bobAnswerId)
                .orElseThrow()
                .getId();

        String allAnswersBody = mockMvc.perform(get("/api/assessment/answers")
                        .param("size", "10")
                        .header("X-User-Id", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(allAnswersBody)
                .contains(aliceAnswerId)
                .contains(bobAnswerId);

        String bobAnswerBody = mockMvc.perform(get("/api/assessment/answers")
                        .param("learnerId", "bob")
                        .header("X-User-Id", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(bobAnswerBody)
                .contains(bobAnswerId)
                .doesNotContain(aliceAnswerId);

        String courseAnswerBody = mockMvc.perform(get("/api/assessment/answers")
                        .param("courseId", "course_admin_answer_list_a")
                        .header("X-User-Id", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(courseAnswerBody)
                .contains(aliceAnswerId)
                .doesNotContain(bobAnswerId);

        String allWrongQuestionsBody = mockMvc.perform(get("/api/assessment/wrong-questions")
                        .param("size", "10")
                        .header("X-User-Id", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(allWrongQuestionsBody)
                .contains(aliceWrongQuestionId)
                .contains(bobWrongQuestionId);

        String bobWrongQuestionBody = mockMvc.perform(get("/api/assessment/wrong-questions")
                        .param("learnerId", "bob")
                        .header("X-User-Id", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(bobWrongQuestionBody)
                .contains(bobWrongQuestionId)
                .doesNotContain(aliceWrongQuestionId);

        String courseWrongQuestionBody = mockMvc.perform(get("/api/assessment/wrong-questions")
                        .param("courseId", "course_admin_answer_list_a")
                        .header("X-User-Id", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(courseWrongQuestionBody)
                .contains(aliceWrongQuestionId)
                .doesNotContain(bobWrongQuestionId);

        mockMvc.perform(get("/api/assessment/answers")
                        .param("courseId", "course_missing_admin_list")
                        .header("X-User-Id", "admin"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/api/assessment/wrong-questions")
                        .param("courseId", "course_missing_admin_list")
                        .header("X-User-Id", "admin"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void assessmentRecordListsValidatePaginationAndReturnStablePageEnvelope() throws Exception {
        String firstAnswerId = submitAnswer("alice", "q_sql_join", "req_alice_page_first");
        String secondAnswerId = submitAnswer("alice", "q_sql_join", "req_alice_page_second");

        String firstPageBody = mockMvc.perform(get("/api/assessment/answers")
                        .param("page", "0")
                        .param("size", "1")
                        .header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondPageBody = mockMvc.perform(get("/api/assessment/answers")
                        .param("page", "1")
                        .param("size", "1")
                        .header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String firstPageAnswerId = objectMapper.readTree(firstPageBody)
                .path("data")
                .path("items")
                .get(0)
                .path("answerId")
                .asText();
        String secondPageAnswerId = objectMapper.readTree(secondPageBody)
                .path("data")
                .path("items")
                .get(0)
                .path("answerId")
                .asText();
        assertThat(firstPageAnswerId).isNotEqualTo(secondPageAnswerId);
        assertThat(java.util.List.of(firstPageAnswerId, secondPageAnswerId))
                .containsExactlyInAnyOrder(firstAnswerId, secondAnswerId);

        mockMvc.perform(get("/api/assessment/answers")
                        .param("page", "-1")
                        .header("X-User-Id", "alice"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/assessment/answers")
                        .param("size", "0")
                        .header("X-User-Id", "alice"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/assessment/answers")
                        .param("size", "51")
                        .header("X-User-Id", "alice"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/assessment/wrong-questions")
                        .param("page", "-1")
                        .header("X-User-Id", "alice"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/assessment/wrong-questions")
                        .param("size", "51")
                        .header("X-User-Id", "alice"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void submitAnswerReplaysSameRequestIdWithoutDuplicatingBusinessRows() throws Exception {
        String requestBody = """
                {
                  "learnerId": "alice",
                  "questionId": "q_sql_join",
                  "answer": "JOIN duplicates happen when a one-to-many relation is joined.",
                  "requestId": "req_answer_idempotent_once"
                }
                """;

        String firstResponseJson = mockMvc.perform(post("/api/assessment/answers")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answerId").isNotEmpty())
                .andExpect(jsonPath("$.data.gradingResultId").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondResponseJson = mockMvc.perform(post("/api/assessment/answers")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode first = objectMapper.readTree(firstResponseJson).path("data");
        JsonNode second = objectMapper.readTree(secondResponseJson).path("data");
        assertThat(second.path("answerId").asText()).isEqualTo(first.path("answerId").asText());
        assertThat(second.path("gradingResultId").asText()).isEqualTo(first.path("gradingResultId").asText());
        assertThat(second.path("traceId").asText()).isEqualTo(first.path("traceId").asText());
        assertThat(second.path("replanRecordId").asText()).isEqualTo(first.path("replanRecordId").asText());

        assertThat(answerRecordRepository.countByLearnerId("alice")).isEqualTo(1);
        assertThat(gradingResultRepository.count()).isEqualTo(1);
        assertThat(masteryRecordRepository.count()).isEqualTo(1);
        assertThat(wrongQuestionRepository.countByLearnerId("alice")).isEqualTo(1);
        assertThat(learningEventRepository.countByLearnerId("alice")).isEqualTo(1);
    }

    @Test
    void submitAnswerRejectsSameRequestIdWithDifferentPayloadWithoutNewRows() throws Exception {
        mockMvc.perform(post("/api/assessment/answers")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "alice",
                                  "questionId": "q_sql_join",
                                  "answer": "JOIN duplicates happen when a one-to-many relation is joined.",
                                  "requestId": "req_answer_conflict"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/assessment/answers")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "alice",
                                  "questionId": "q_sql_join",
                                  "answer": "A different answer tries to reuse the same request id.",
                                  "requestId": "req_answer_conflict"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        assertThat(answerRecordRepository.countByLearnerId("alice")).isEqualTo(1);
        assertThat(gradingResultRepository.count()).isEqualTo(1);
        assertThat(masteryRecordRepository.count()).isEqualTo(1);
        assertThat(wrongQuestionRepository.countByLearnerId("alice")).isEqualTo(1);
        assertThat(learningEventRepository.countByLearnerId("alice")).isEqualTo(1);
    }

    @Test
    void submitAnswerScopesRequestIdByLearner() throws Exception {
        String requestTemplate = """
                {
                  "learnerId": "%s",
                  "questionId": "q_sql_join",
                  "answer": "JOIN duplicates happen when a one-to-many relation is joined.",
                  "requestId": "req_shared_by_two_learners"
                }
                """;

        String aliceResponseJson = mockMvc.perform(post("/api/assessment/answers")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestTemplate.formatted("alice")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String bobResponseJson = mockMvc.perform(post("/api/assessment/answers")
                        .header("X-User-Id", "bob")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestTemplate.formatted("bob")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode alice = objectMapper.readTree(aliceResponseJson).path("data");
        JsonNode bob = objectMapper.readTree(bobResponseJson).path("data");
        assertThat(bob.path("answerId").asText()).isNotEqualTo(alice.path("answerId").asText());
        assertThat(answerRecordRepository.countByLearnerId("alice")).isEqualTo(1);
        assertThat(answerRecordRepository.countByLearnerId("bob")).isEqualTo(1);
        assertThat(wrongQuestionRepository.countByLearnerId("alice")).isEqualTo(1);
        assertThat(wrongQuestionRepository.countByLearnerId("bob")).isEqualTo(1);
    }

    @Test
    void submitAnswerRejectsMissingRequestId() throws Exception {
        mockMvc.perform(post("/api/assessment/answers")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "alice",
                                  "questionId": "q_sql_join",
                                  "answer": "JOIN duplicates happen when a one-to-many relation is joined."
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void submitAnswerRejectsBearerStudentForeignCourseQuestionIdWithoutSideEffects() throws Exception {
        seedCourseKnowledgeAndEnrollment(
                "course_answer_submit_alice",
                "teacher_a",
                "alice",
                "Alice authorized SQL JOIN"
        );
        String foreignQuestionId = seedCourseKnowledgeAndEnrollment(
                "course_answer_submit_bob",
                "teacher_b",
                "bob",
                "Bob foreign SQL window"
        );

        long answerCount = answerRecordRepository.count();
        long gradingCount = gradingResultRepository.count();
        long masteryCount = masteryRecordRepository.count();
        long wrongQuestionCount = wrongQuestionRepository.count();
        long eventCount = learningEventRepository.count();

        String body = mockMvc.perform(post("/api/assessment/answers")
                        .header("Authorization", "Bearer " + jwt("alice", "Alice", List.of("USER")))
                        .header("X-User-Id", "teacher_b")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "alice",
                                  "questionId": "%s",
                                  "answer": "A forged foreign course answer should not persist.",
                                  "requestId": "req_answer_foreign_questionid"
                                }
                                """.formatted(foreignQuestionId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .doesNotContain(foreignQuestionId)
                .doesNotContain("course_answer_submit_bob")
                .doesNotContain("Bob foreign SQL window")
                .doesNotContain("req_answer_foreign_questionid");
        assertThat(answerRecordRepository.count()).isEqualTo(answerCount);
        assertThat(gradingResultRepository.count()).isEqualTo(gradingCount);
        assertThat(masteryRecordRepository.count()).isEqualTo(masteryCount);
        assertThat(wrongQuestionRepository.count()).isEqualTo(wrongQuestionCount);
        assertThat(learningEventRepository.count()).isEqualTo(eventCount);
    }

    @Test
    void exposesAutomatedGradingQualityEvaluationReport() throws Exception {
        String questionId = seedCourseKnowledgeAndEnrollment(
                "course_grading_eval_report",
                "teacher",
                "learner_grading_eval_report",
                "SQL JOIN grading evaluation"
        );
        String knowledgePointId = "kp_" + questionId.substring("q_".length());

        String responseJson = mockMvc.perform(post("/api/assessment/grading-evaluations")
                        .header("X-User-Id", "teacher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseId": "course_grading_eval_report",
                                  "samples": [
                                    {
                                      "sampleId": "sample_1",
                                      "questionType": "SHORT_ANSWER",
                                      "knowledgePointId": "%s",
                                      "rubricVersion": "rubric-v1",
                                      "humanScore": 0.90,
                                      "systemScore": 0.80,
                                      "humanGrade": "A",
                                      "systemGrade": "B",
                                      "humanWrongCause": "TRANSFER_WEAKNESS",
                                      "systemWrongCause": "TRANSFER_WEAKNESS"
                                    },
                                    {
                                      "sampleId": "sample_2",
                                      "questionType": "SHORT_ANSWER",
                                      "knowledgePointId": "%s",
                                      "rubricVersion": "rubric-v1",
                                      "humanScore": 0.70,
                                      "systemScore": 0.75,
                                      "humanGrade": "B",
                                      "systemGrade": "B",
                                      "humanWrongCause": "STEP_ERROR",
                                      "systemWrongCause": "STEP_ERROR"
                                    },
                                    {
                                      "sampleId": "sample_3",
                                      "questionType": "CHOICE",
                                      "knowledgePointId": "",
                                      "rubricVersion": "rubric-v2",
                                      "humanScore": 0.40,
                                      "systemScore": 0.55,
                                      "humanGrade": "C",
                                      "systemGrade": "C",
                                      "humanWrongCause": "CONCEPT_ERROR",
                                      "systemWrongCause": "STEP_ERROR"
                                    },
                                    {
                                      "sampleId": "sample_4",
                                      "questionType": "CHOICE",
                                      "knowledgePointId": "",
                                      "rubricVersion": "rubric-v2",
                                      "humanScore": 0.60,
                                      "systemScore": 0.50,
                                      "humanGrade": "B",
                                      "systemGrade": "C",
                                      "humanWrongCause": "INCOMPLETE_EXPRESSION",
                                      "systemWrongCause": "INCOMPLETE_EXPRESSION"
                                    }
                                  ]
                                }
                                """.formatted(knowledgePointId, knowledgePointId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.sampleCount").value(4))
                .andExpect(jsonPath("$.data.groupedAnalysis.questionType.length()").value(2))
                .andExpect(jsonPath("$.data.groupedAnalysis.questionType[0].groupKey").value("SHORT_ANSWER"))
                .andExpect(jsonPath("$.data.groupedAnalysis.questionType[0].sampleCount").value(2))
                .andExpect(jsonPath("$.data.groupedAnalysis.knowledgePointId[1].groupKey").value("UNKNOWN"))
                .andExpect(jsonPath("$.data.groupedAnalysis.rubricVersion[1].groupKey").value("rubric-v2"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseJson);
        assertThat(response.path("data").path("meanAbsoluteError").asDouble())
                .isCloseTo(0.10, org.assertj.core.data.Offset.offset(0.0000001));
        assertThat(response.path("data").path("agreementRate").asDouble())
                .isCloseTo(0.50, org.assertj.core.data.Offset.offset(0.0000001));
        assertThat(response.path("data").path("gradeAgreementRate").asDouble())
                .isCloseTo(0.50, org.assertj.core.data.Offset.offset(0.0000001));
        assertThat(response.path("data").path("wrongCauseAgreementRate").asDouble())
                .isCloseTo(0.75, org.assertj.core.data.Offset.offset(0.0000001));
        assertThat(response.path("data").path("groupedAnalysis").path("questionType").get(0)
                .path("wrongCauseAgreementRate").asDouble())
                .isCloseTo(1.00, org.assertj.core.data.Offset.offset(0.0000001));
    }

    @Test
    void rejectsTeacherGradingEvaluationWithoutCourseId() throws Exception {
        mockMvc.perform(post("/api/assessment/grading-evaluations")
                        .header("X-User-Id", "teacher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "humanScores": [1.0],
                                  "aiScores": [0.96],
                                  "agreementThreshold": 0.05
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void teacherCanOnlyRunGradingEvaluationForOwnCourse() throws Exception {
        String ownQuestionId = seedCourseKnowledgeAndEnrollment(
                "course_teacher_a_grading_eval",
                "teacher_a",
                "learner_grading_eval_a",
                "Teacher A grading evaluation"
        );
        seedCourseKnowledgeAndEnrollment(
                "course_teacher_b_grading_eval",
                "teacher_b",
                "learner_grading_eval_b",
                "Teacher B grading evaluation"
        );
        String ownKnowledgePointId = "kp_" + ownQuestionId.substring("q_".length());

        mockMvc.perform(post("/api/assessment/grading-evaluations")
                        .header("X-User-Id", "teacher_a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseId": "course_teacher_a_grading_eval",
                                  "samples": [
                                    {
                                      "sampleId": "sample_own_course",
                                      "questionType": "SHORT_ANSWER",
                                      "knowledgePointId": "%s",
                                      "rubricVersion": "rubric-v1",
                                      "humanScore": 0.90,
                                      "systemScore": 0.80,
                                      "humanGrade": "A",
                                      "systemGrade": "B",
                                      "humanWrongCause": "TRANSFER_WEAKNESS",
                                      "systemWrongCause": "TRANSFER_WEAKNESS"
                                    }
                                  ]
                                }
                                """.formatted(ownKnowledgePointId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.sampleCount").value(1));

        mockMvc.perform(post("/api/assessment/grading-evaluations")
                        .header("X-User-Id", "teacher_a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseId": "course_teacher_b_grading_eval",
                                  "humanScores": [1.0],
                                  "aiScores": [0.96]
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void teacherCannotDistinguishMissingCourseFromForbiddenGradingEvaluationCourse() throws Exception {
        mockMvc.perform(post("/api/assessment/grading-evaluations")
                        .header("X-User-Id", "teacher_a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseId": "course_missing_grading_eval",
                                  "humanScores": [1.0],
                                  "aiScores": [0.96]
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void adminCanRunGradingEvaluationForExistingCourse() throws Exception {
        seedCourseKnowledgeAndEnrollment(
                "course_admin_grading_eval",
                "teacher_admin_eval",
                "learner_admin_eval",
                "Admin grading evaluation"
        );

        mockMvc.perform(post("/api/assessment/grading-evaluations")
                        .header("X-User-Id", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseId": "course_admin_grading_eval",
                                  "humanScores": [1.0, 0.7],
                                  "aiScores": [0.96, 0.72],
                                  "agreementThreshold": 0.05
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.sampleCount").value(2))
                .andExpect(jsonPath("$.data.agreementRate").value(1.0));
    }

    @Test
    void rejectsAdminGradingEvaluationWithoutCourseId() throws Exception {
        mockMvc.perform(post("/api/assessment/grading-evaluations")
                        .header("X-User-Id", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "humanScores": [1.0],
                                  "aiScores": [0.96]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void adminMissingGradingEvaluationCourseReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/assessment/grading-evaluations")
                        .header("X-User-Id", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseId": "course_missing_admin_grading_eval",
                                  "humanScores": [1.0],
                                  "aiScores": [0.96]
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void gradingEvaluationUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader() throws Exception {
        seedCourseKnowledgeAndEnrollment(
                "course_bearer_admin_grading_eval",
                "teacher_bearer_admin_eval",
                "learner_bearer_admin_eval",
                "Bearer admin grading evaluation"
        );

        mockMvc.perform(post("/api/assessment/grading-evaluations")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseId": "course_bearer_admin_grading_eval",
                                  "humanScores": [1.0, 0.7],
                                  "aiScores": [0.96, 0.72],
                                  "agreementThreshold": 0.05
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.sampleCount").value(2));
    }

    @Test
    void gradingEvaluationBearerAdminMissingCourseReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/assessment/grading-evaluations")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseId": "course_missing_bearer_admin_grading_eval",
                                  "humanScores": [1.0],
                                  "aiScores": [0.96]
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void gradingEvaluationAllowsBearerTeacherWithoutSubjectPrefixForOwnCourse() throws Exception {
        seedCourseKnowledgeAndEnrollment(
                "course_bearer_instructor_grading_eval",
                "instructor_1",
                "learner_bearer_instructor_eval",
                "Bearer instructor grading evaluation"
        );

        mockMvc.perform(post("/api/assessment/grading-evaluations")
                        .header("Authorization", "Bearer " + jwt("instructor_1", "Instructor", List.of("TEACHER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseId": "course_bearer_instructor_grading_eval",
                                  "humanScores": [1.0],
                                  "aiScores": [0.96]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.sampleCount").value(1));
    }

    @Test
    void gradingEvaluationRejectsBearerUserSubjectAdminRoleConfusion() throws Exception {
        seedCourseKnowledgeAndEnrollment(
                "course_user_subject_admin_grading_eval",
                "teacher_user_subject_admin_eval",
                "learner_user_subject_admin_eval",
                "User subject admin grading evaluation"
        );

        mockMvc.perform(post("/api/assessment/grading-evaluations")
                        .header("Authorization", "Bearer " + jwt("admin", "Subject Admin", List.of("USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseId": "course_user_subject_admin_grading_eval",
                                  "humanScores": [1.0],
                                  "aiScores": [0.96]
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void gradingEvaluationRejectsBearerUserSubjectTeacherPrefixRoleConfusion() throws Exception {
        seedCourseKnowledgeAndEnrollment(
                "course_user_subject_teacher_grading_eval",
                "teacher_1",
                "learner_user_subject_teacher_eval",
                "User subject teacher grading evaluation"
        );

        mockMvc.perform(post("/api/assessment/grading-evaluations")
                        .header("Authorization", "Bearer " + jwt("teacher_1", "Subject Teacher", List.of("USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseId": "course_user_subject_teacher_grading_eval",
                                  "humanScores": [1.0],
                                  "aiScores": [0.96]
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void rejectsGradingEvaluationSampleOutsideRequestCourse() throws Exception {
        String ownQuestionId = seedCourseKnowledgeAndEnrollment(
                "course_grading_eval_sample_owner",
                "teacher_a",
                "learner_grading_eval_sample_owner",
                "Own grading evaluation sample"
        );
        String foreignQuestionId = seedCourseKnowledgeAndEnrollment(
                "course_grading_eval_sample_foreign",
                "teacher_b",
                "learner_grading_eval_sample_foreign",
                "Foreign grading evaluation sample"
        );
        String ownKnowledgePointId = "kp_" + ownQuestionId.substring("q_".length());
        String foreignKnowledgePointId = "kp_" + foreignQuestionId.substring("q_".length());

        mockMvc.perform(post("/api/assessment/grading-evaluations")
                        .header("X-User-Id", "teacher_a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseId": "course_grading_eval_sample_owner",
                                  "samples": [
                                    {
                                      "sampleId": "sample_own_course",
                                      "questionType": "SHORT_ANSWER",
                                      "knowledgePointId": "%s",
                                      "rubricVersion": "rubric-v1",
                                      "humanScore": 0.90,
                                      "systemScore": 0.80,
                                      "humanGrade": "A",
                                      "systemGrade": "B",
                                      "humanWrongCause": "TRANSFER_WEAKNESS",
                                      "systemWrongCause": "TRANSFER_WEAKNESS"
                                    },
                                    {
                                      "sampleId": "sample_foreign_course",
                                      "questionType": "SHORT_ANSWER",
                                      "knowledgePointId": "%s",
                                      "rubricVersion": "rubric-v1",
                                      "humanScore": 0.70,
                                      "systemScore": 0.75,
                                      "humanGrade": "B",
                                      "systemGrade": "B",
                                      "humanWrongCause": "STEP_ERROR",
                                      "systemWrongCause": "STEP_ERROR"
                                    }
                                  ]
                                }
                                """.formatted(ownKnowledgePointId, foreignKnowledgePointId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.message").value("Sample knowledge points must belong to request course"));
    }

    @Test
    void rejectsStudentRunningGradingEvaluation() throws Exception {
        seedCourseKnowledgeAndEnrollment(
                "course_student_grading_eval_forbidden",
                "teacher",
                "alice",
                "Student grading evaluation forbidden"
        );

        mockMvc.perform(post("/api/assessment/grading-evaluations")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseId": "course_student_grading_eval_forbidden",
                                  "samples": [
                                    {
                                      "sampleId": "sample_forbidden",
                                      "questionType": "SHORT_ANSWER",
                                      "knowledgePointId": "kp_sql_join",
                                      "rubricVersion": "rubric-v1",
                                      "humanScore": 0.90,
                                      "systemScore": 0.80,
                                      "humanGrade": "A",
                                      "systemGrade": "B",
                                      "humanWrongCause": "TRANSFER_WEAKNESS",
                                      "systemWrongCause": "TRANSFER_WEAKNESS"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    private String submitAnswer(String learnerId, String questionId, String requestId) throws Exception {
        String responseJson = mockMvc.perform(post("/api/assessment/answers")
                        .header("X-User-Id", learnerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "%s",
                                  "questionId": "%s",
                                  "answer": "JOIN duplicates happen when a one-to-many relation is joined.",
                                  "requestId": "%s"
                                }
                                """.formatted(learnerId, questionId, requestId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(responseJson).path("data").path("answerId").asText();
    }

    private String wrongQuestionIdForAnswer(String answerId) {
        return wrongQuestionRepository
                .findFirstByAnswerIdOrderByCreatedAtDesc(answerId)
                .orElseThrow()
                .getId();
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

    private String seedCourseKnowledgeAndEnrollment(
            String courseId,
            String teacherId,
            String learnerId,
            String knowledgePointTitle
    ) {
        Course course = new Course();
        course.setId(courseId);
        course.setTitle("Course " + courseId);
        course.setDescription("Course for assessment RBAC tests.");
        course.setTeacherId(teacherId);
        course.setStatus("PUBLISHED");
        courseRepository.save(course);

        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setCourseId(courseId);
        enrollment.setLearnerId(learnerId);
        enrollment.setStatus("ACTIVE");
        courseEnrollmentRepository.save(enrollment);

        KnowledgePoint point = new KnowledgePoint();
        point.setCourseId(courseId);
        point.setChapterId("chapter_" + courseId);
        point.setTitle(knowledgePointTitle);
        point.setDescription("Assessment RBAC linked knowledge point.");
        KnowledgePoint savedPoint = knowledgePointRepository.save(point);
        return "q_" + savedPoint.getId().substring("kp_".length());
    }
}
