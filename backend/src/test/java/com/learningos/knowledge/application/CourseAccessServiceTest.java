package com.learningos.knowledge.application;

import com.learningos.common.api.ErrorCode;
import com.learningos.common.exception.ApiException;
import com.learningos.knowledge.domain.Course;
import com.learningos.knowledge.dto.KnowledgeDtos.CreateChapterRequest;
import com.learningos.knowledge.dto.KnowledgeDtos.CreateCourseRequest;
import com.learningos.knowledge.dto.KnowledgeDtos.CreateKnowledgeDependencyRequest;
import com.learningos.knowledge.dto.KnowledgeDtos.CreateKnowledgePointRequest;
import com.learningos.knowledge.repository.CourseEnrollmentRepository;
import com.learningos.knowledge.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CourseAccessServiceTest {

    private final CourseAccessService courseAccessService;
    private final CourseRepository courseRepository;
    @SuppressWarnings("unused")
    private final CourseEnrollmentRepository courseEnrollmentRepository;

    CourseAccessServiceTest(
            CourseAccessService courseAccessService,
            CourseRepository courseRepository,
            CourseEnrollmentRepository courseEnrollmentRepository
    ) {
        this.courseAccessService = courseAccessService;
        this.courseRepository = courseRepository;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
    }

    @Test
    void doesNotExposeLegacySubjectNameRoleOverloads() {
        assertNoDeclaredMethod("requireCourseRead", String.class, String.class);
        assertNoDeclaredMethod("requireCourseManage", String.class, Course.class);
        assertNoDeclaredMethod(
                "requireLearnerEnrolledForExistingCourse",
                String.class,
                String.class,
                String.class
        );
        assertNoDeclaredMethod("listCoursesForUser", String.class);
    }

    @Test
    void doesNotKeepSubjectNameInferenceHelpers() {
        assertNoDeclaredMethod("scopedCourseMissing", String.class);
        assertNoDeclaredMethod("isAdmin", String.class);
        assertNoDeclaredMethod("isTeacherUser", String.class);
    }

    @Test
    void knowledgeCatalogServiceDoesNotExposeLegacySubjectNameRoleOverloads() {
        assertNoDeclaredMethod(KnowledgeCatalogService.class, "createCourse", String.class, CreateCourseRequest.class);
        assertNoDeclaredMethod(KnowledgeCatalogService.class, "getCourseForUser", String.class, String.class);
        assertNoDeclaredMethod(KnowledgeCatalogService.class, "listCoursesForUser", String.class);
        assertNoDeclaredMethod(
                KnowledgeCatalogService.class,
                "createChapter",
                String.class,
                String.class,
                CreateChapterRequest.class
        );
        assertNoDeclaredMethod(
                KnowledgeCatalogService.class,
                "createKnowledgePoint",
                String.class,
                CreateKnowledgePointRequest.class
        );
        assertNoDeclaredMethod(
                KnowledgeCatalogService.class,
                "createDependency",
                String.class,
                CreateKnowledgeDependencyRequest.class
        );
        assertNoDeclaredMethod(KnowledgeCatalogService.class, "getKnowledgeGraphForUser", String.class, String.class);
    }

    @Test
    void knowledgeCatalogServiceDoesNotKeepSubjectNameInferenceHelpers() {
        assertNoDeclaredMethod(KnowledgeCatalogService.class, "resolveCourseTeacherId", String.class, String.class);
        assertNoDeclaredMethod(KnowledgeCatalogService.class, "requireCourseTeacherOrAdmin", String.class, Course.class);
        assertNoDeclaredMethod(KnowledgeCatalogService.class, "requireCourseManageAccess", String.class, Course.class);
        assertNoDeclaredMethod(KnowledgeCatalogService.class, "requireCourseReadAccess", String.class, Course.class);
        assertNoDeclaredMethod(KnowledgeCatalogService.class, "scopedCourseMissing", String.class);
        assertNoDeclaredMethod(KnowledgeCatalogService.class, "isAdmin", String.class);
        assertNoDeclaredMethod(KnowledgeCatalogService.class, "isTeacherUser", String.class);
    }

    @Test
    void rolesFirstCourseReadDoesNotTreatAdminSubjectAsAdminWithoutAdminRole() {
        assertThatThrownBy(() ->
                courseAccessService.requireCourseRead("admin", false, false, "course_missing_subject_admin"))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void rolesFirstCourseManageDoesNotTreatTeacherSubjectAsTeacherWithoutTeacherRole() {
        Course course = saveCourse("teacher_1", "Teacher Subject Course");

        assertThatThrownBy(() ->
                courseAccessService.requireCourseManage("teacher_1", false, false, course))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    private void assertNoDeclaredMethod(String methodName, Class<?>... parameterTypes) {
        assertNoDeclaredMethod(CourseAccessService.class, methodName, parameterTypes);
    }

    private void assertNoDeclaredMethod(Class<?> type, String methodName, Class<?>... parameterTypes) {
        assertThat(Arrays.stream(type.getDeclaredMethods())
                .filter(method -> method.getName().equals(methodName))
                .filter(method -> Arrays.equals(method.getParameterTypes(), parameterTypes))
                .toList())
                .as("%s should not expose %s%s", type.getSimpleName(), methodName, Arrays.toString(parameterTypes))
                .isEmpty();
    }

    private Course saveCourse(String teacherId, String title) {
        Course course = new Course();
        course.setTitle(title);
        course.setDescription("Course access service test course.");
        course.setTeacherId(teacherId);
        course.setStatus("DRAFT");
        return courseRepository.save(course);
    }
}
