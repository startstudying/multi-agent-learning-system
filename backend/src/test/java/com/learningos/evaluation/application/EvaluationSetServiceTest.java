package com.learningos.evaluation.application;

import com.learningos.common.api.ErrorCode;
import com.learningos.common.exception.ApiException;
import com.learningos.evaluation.dto.EvaluationSampleRequest;
import com.learningos.evaluation.dto.EvaluationSetUpsertRequest;
import com.learningos.evaluation.repository.EvaluationSampleRepository;
import com.learningos.evaluation.repository.EvaluationSetRepository;
import com.learningos.knowledge.domain.Course;
import com.learningos.knowledge.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(EvaluationSetService.class)
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class EvaluationSetServiceTest {

    private final EvaluationSetService evaluationSetService;
    private final EvaluationSetRepository evaluationSetRepository;
    private final EvaluationSampleRepository evaluationSampleRepository;
    private final CourseRepository courseRepository;

    EvaluationSetServiceTest(
            EvaluationSetService evaluationSetService,
            EvaluationSetRepository evaluationSetRepository,
            EvaluationSampleRepository evaluationSampleRepository,
            CourseRepository courseRepository
    ) {
        this.evaluationSetService = evaluationSetService;
        this.evaluationSetRepository = evaluationSetRepository;
        this.evaluationSampleRepository = evaluationSampleRepository;
        this.courseRepository = courseRepository;
    }

    @Test
    void createsRagQuestionSetWithExpectedSources() {
        var response = evaluationSetService.upsert("teacher", false, true, new EvaluationSetUpsertRequest(
                "rag-sql-benchmark",
                "v1",
                "SQL RAG benchmark",
                "Course RAG benchmark",
                "RAG_QUESTION",
                "active",
                null,
                null,
                "rag-answer",
                "agent-rag-v1",
                List.of(new EvaluationSampleRequest(
                        "rag-001",
                        "How do JOIN duplicates happen?",
                        List.of("doc_sql_join", "doc_sql_relation"),
                        3,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of("must cite SQL JOIN source")
                ))
        ));

        assertThat(response.id()).startsWith("evs_");
        assertThat(response.type()).isEqualTo("RAG_QUESTION");
        assertThat(response.sampleCount()).isEqualTo(1);
        assertThat(response.samples()).singleElement().satisfies(sample -> {
            assertThat(sample.id()).startsWith("evsm_");
            assertThat(sample.sampleKey()).isEqualTo("rag-001");
            assertThat(sample.question()).isEqualTo("How do JOIN duplicates happen?");
            assertThat(sample.expectedSourceIds()).containsExactly("doc_sql_join", "doc_sql_relation");
            assertThat(sample.topK()).isEqualTo(3);
        });
        assertThat(evaluationSampleRepository.countBySetId(response.id())).isEqualTo(1);
    }

    @Test
    void createsGradingSampleSetWithGoldScores() {
        var response = evaluationSetService.upsert("teacher", false, true, new EvaluationSetUpsertRequest(
                "grading-sql-open-ended",
                "v1",
                "SQL grading samples",
                "Human-scored open ended answers",
                "GRADING_SAMPLE",
                "ACTIVE",
                null,
                null,
                "assessment-grading",
                "agent-grading-v1",
                List.of(new EvaluationSampleRequest(
                        "grade-001",
                        null,
                        List.of(),
                        null,
                        "q_sql_join",
                        "JOIN duplicates happen when one row matches many rows.",
                        "Score conceptual correctness and missing prerequisite explanation.",
                        0.85,
                        null,
                        null,
                        null,
                        List.of("wrong-cause label should match")
                ))
        ));

        assertThat(response.type()).isEqualTo("GRADING_SAMPLE");
        assertThat(response.samples()).singleElement().satisfies(sample -> {
            assertThat(sample.questionId()).isEqualTo("q_sql_join");
            assertThat(sample.answerText()).contains("JOIN duplicates");
            assertThat(sample.rubric()).contains("conceptual correctness");
            assertThat(sample.humanScore()).isEqualTo(0.85);
        });
    }

    @Test
    void createsResourceGenerationSampleSetWithExpectedCriteria() {
        var response = evaluationSetService.upsert("teacher", false, true, new EvaluationSetUpsertRequest(
                "resource-generation-remediation",
                "v1",
                "Resource generation benchmark",
                "Profile-aware remedial resource samples",
                "RESOURCE_GENERATION_SAMPLE",
                "ACTIVE",
                null,
                null,
                "resource-generation",
                "agent-resource-v1",
                List.of(new EvaluationSampleRequest(
                        "resource-001",
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        "{\"base_level\":\"beginner\",\"weak_points\":[\"JOIN\"]}",
                        "Master SQL JOIN diagnosis",
                        "REMEDIAL_PRACTICE",
                        List.of("must include citations", "must include practice task")
                ))
        ));

        assertThat(response.type()).isEqualTo("RESOURCE_GENERATION_SAMPLE");
        assertThat(response.samples()).singleElement().satisfies(sample -> {
            assertThat(sample.learnerProfileSnapshot()).contains("beginner");
            assertThat(sample.learningGoal()).isEqualTo("Master SQL JOIN diagnosis");
            assertThat(sample.expectedResourceType()).isEqualTo("REMEDIAL_PRACTICE");
            assertThat(sample.qualityCriteria()).containsExactly("must include citations", "must include practice task");
        });
    }

    @Test
    void duplicateCodeAndVersionUpdatesExistingEvaluationSetWithoutCreatingDuplicateRows() {
        var first = evaluationSetService.upsert("teacher", false, true, minimalRagSet("Original name"));
        var updated = evaluationSetService.upsert("teacher", false, true, minimalRagSet("Updated name"));

        assertThat(updated.id()).isEqualTo(first.id());
        assertThat(updated.name()).isEqualTo("Updated name");
        assertThat(evaluationSetRepository.count()).isEqualTo(1);
        assertThat(evaluationSampleRepository.countBySetId(updated.id())).isEqualTo(1);
    }

    @Test
    void rejectsSamplePayloadThatDoesNotMatchSetType() {
        assertThatThrownBy(() -> evaluationSetService.upsert("teacher", false, true, new EvaluationSetUpsertRequest(
                "rag-invalid",
                "v1",
                "Invalid RAG set",
                null,
                "RAG_QUESTION",
                "ACTIVE",
                null,
                null,
                null,
                null,
                List.of(new EvaluationSampleRequest(
                        "invalid",
                        null,
                        List.of(),
                        null,
                        "q_sql",
                        "answer text",
                        "rubric",
                        0.8,
                        null,
                        null,
                        null,
                        List.of()
                ))
        )))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> {
                    ApiException apiException = (ApiException) error;
                    assertThat(apiException.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    assertThat(apiException.getMessage()).isEqualTo("Evaluation sample payload does not match set type");
                });
    }

    @Test
    void studentCannotManageEvaluationSets() {
        assertThatThrownBy(() -> evaluationSetService.upsert("student", false, false, minimalRagSet("Student set")))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void teacherCannotBindEvaluationSetToForeignCourse() {
        Course course = new Course();
        course.setId("course_foreign");
        course.setTitle("Foreign course");
        course.setTeacherId("other_teacher");
        course.setStatus("ACTIVE");
        courseRepository.save(course);

        assertThatThrownBy(() -> evaluationSetService.upsert("teacher", false, true, minimalRagSetForCourse("Foreign course set", "course_foreign")))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> {
                    ApiException apiException = (ApiException) error;
                    assertThat(apiException.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
                    assertThat(apiException.getMessage()).isEqualTo("Evaluation set access denied");
                });
    }

    private EvaluationSetUpsertRequest minimalRagSet(String name) {
        return minimalRagSetForCourse(name, null);
    }

    private EvaluationSetUpsertRequest minimalRagSetForCourse(String name, String courseId) {
        return new EvaluationSetUpsertRequest(
                "rag-duplicate",
                "v1",
                name,
                "Duplicate upsert benchmark",
                "RAG_QUESTION",
                "ACTIVE",
                courseId,
                null,
                "rag-answer",
                "agent-rag-v1",
                List.of(new EvaluationSampleRequest(
                        "rag-duplicate-001",
                        "What is the difference between INNER JOIN and LEFT JOIN?",
                        List.of("doc_join"),
                        2,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of("must cite join source")
                ))
        );
    }
}
