package com.learningos.analytics.application;

import com.learningos.agent.repository.AgentTaskRepository;
import com.learningos.agent.repository.AgentTraceRepository;
import com.learningos.agent.repository.LearningResourceRepository;
import com.learningos.agent.repository.ModelCallLogRepository;
import com.learningos.agent.repository.ResourceGenerationTaskRepository;
import com.learningos.agent.repository.ResourceReviewRepository;
import com.learningos.agent.repository.TokenUsageLogRepository;
import com.learningos.assessment.repository.AnswerRecordRepository;
import com.learningos.assessment.repository.WrongQuestionRepository;
import com.learningos.common.api.ErrorCode;
import com.learningos.common.exception.ApiException;
import com.learningos.knowledge.domain.Course;
import com.learningos.knowledge.repository.CourseRepository;
import com.learningos.knowledge.repository.KnowledgePointRepository;
import com.learningos.learning.domain.LearningPath;
import com.learningos.learning.repository.LearningEventRepository;
import com.learningos.learning.repository.LearningPathNodeRepository;
import com.learningos.learning.repository.LearningPathRepository;
import com.learningos.learning.repository.MasteryRecordRepository;
import com.learningos.rag.repository.KbQueryLogRepository;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalyticsServiceTest {

    private final AgentTaskRepository agentTaskRepository = mock(AgentTaskRepository.class);
    private final AgentTraceRepository agentTraceRepository = mock(AgentTraceRepository.class);
    private final ModelCallLogRepository modelCallLogRepository = mock(ModelCallLogRepository.class);
    private final TokenUsageLogRepository tokenUsageLogRepository = mock(TokenUsageLogRepository.class);
    private final AnswerRecordRepository answerRecordRepository = mock(AnswerRecordRepository.class);
    private final WrongQuestionRepository wrongQuestionRepository = mock(WrongQuestionRepository.class);
    private final LearningEventRepository learningEventRepository = mock(LearningEventRepository.class);
    private final LearningPathRepository learningPathRepository = mock(LearningPathRepository.class);
    private final LearningPathNodeRepository learningPathNodeRepository = mock(LearningPathNodeRepository.class);
    private final MasteryRecordRepository masteryRecordRepository = mock(MasteryRecordRepository.class);
    private final ResourceReviewRepository resourceReviewRepository = mock(ResourceReviewRepository.class);
    private final KbQueryLogRepository kbQueryLogRepository = mock(KbQueryLogRepository.class);
    private final CourseRepository courseRepository = mock(CourseRepository.class);
    private final KnowledgePointRepository knowledgePointRepository = mock(KnowledgePointRepository.class);
    private final ResourceGenerationTaskRepository resourceGenerationTaskRepository = mock(ResourceGenerationTaskRepository.class);
    private final LearningResourceRepository learningResourceRepository = mock(LearningResourceRepository.class);

    @Test
    void teacherClassSummaryLegacyOverloadIsRemoved() {
        assertThat(Arrays.stream(AnalyticsService.class.getMethods())
                .noneMatch(method -> method.getName().equals("teacherClassSummary")
                        && Arrays.equals(method.getParameterTypes(), new Class<?>[]{
                        String.class,
                        String.class
                }))).isTrue();
    }

    @Test
    void analyticsServiceLegacyTeacherHelperIsRemoved() {
        assertThat(Arrays.stream(AnalyticsService.class.getDeclaredMethods())
                .noneMatch(method -> method.getName().equals("isLegacyTeacherUser")
                        && Arrays.equals(method.getParameterTypes(), new Class<?>[]{String.class}))).isTrue();
    }

    @Test
    void rolesFirstTeacherClassSummaryDoesNotGrantAdminWhenOnlySubjectNameIsAdmin() {
        AnalyticsService service = serviceWithoutCourseAccess();
        when(courseRepository.findById("course_backend")).thenReturn(Optional.of(course("course_backend", "teacher")));

        assertThatThrownBy(() -> service.teacherClassSummary("course_backend", "admin", false, false))
                .isInstanceOfSatisfying(ApiException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void rolesFirstTeacherClassSummaryDoesNotGrantTeacherWhenOnlySubjectOwnsCourse() {
        AnalyticsService service = serviceWithoutCourseAccess();
        when(courseRepository.findById("course_backend")).thenReturn(Optional.of(course("course_backend", "teacher")));

        assertThatThrownBy(() -> service.teacherClassSummary("course_backend", "teacher", false, false))
                .isInstanceOfSatisfying(ApiException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void teacherClassSummaryDoesNotInferClassMembershipFromLearningPathsWhenCourseAccessServiceIsMissing() {
        AnalyticsService service = serviceWithoutCourseAccess();
        when(courseRepository.findById("course_backend")).thenReturn(Optional.of(course("course_backend", "teacher")));
        LearningPath legacyPath = new LearningPath();
        legacyPath.setId("path_legacy");
        legacyPath.setLearnerId("alice");
        legacyPath.setGoalId("course_backend");
        when(learningPathRepository.findAll()).thenReturn(List.of(legacyPath));
        when(knowledgePointRepository.findByCourseIdOrderByCreatedAtAsc("course_backend")).thenReturn(List.of());

        AnalyticsService.TeacherClassAnalyticsSummary summary = service.teacherClassSummary(
                "course_backend",
                "teacher",
                false,
                true
        );

        assertThat(summary.learnerCount()).isZero();
    }

    private AnalyticsService serviceWithoutCourseAccess() {
        return new AnalyticsService(
                agentTaskRepository,
                agentTraceRepository,
                modelCallLogRepository,
                tokenUsageLogRepository,
                answerRecordRepository,
                wrongQuestionRepository,
                learningEventRepository,
                learningPathRepository,
                learningPathNodeRepository,
                masteryRecordRepository,
                resourceReviewRepository,
                kbQueryLogRepository,
                courseRepository,
                knowledgePointRepository,
                resourceGenerationTaskRepository,
                learningResourceRepository
        );
    }

    private Course course(String courseId, String teacherId) {
        Course course = new Course();
        course.setId(courseId);
        course.setTitle("Backend course");
        course.setTeacherId(teacherId);
        return course;
    }
}
