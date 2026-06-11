package com.learningos.knowledge.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningos.knowledge.domain.CourseEnrollment;
import com.learningos.knowledge.repository.CourseEnrollmentRepository;
import com.learningos.knowledge.repository.CourseRepository;
import com.learningos.knowledge.repository.KnowledgeDependencyRepository;
import com.learningos.knowledge.repository.KnowledgePointRepository;
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
class CourseKnowledgeControllerTest {

    private static final String AUTH_SECRET = "unit-test-secret-32-bytes-long!!";
    private static final String AUTH_ISSUER = "learning-os";
    private static final String TEACHER_ID = "teacher_1";
    private static final String OTHER_TEACHER_ID = "teacher_2";
    private static final String INSTRUCTOR_ID = "instructor_1";
    private static final String STUDENT_ID = "alice";
    private static final String ADMIN_ID = "admin";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final CourseRepository courseRepository;
    private final KnowledgePointRepository knowledgePointRepository;
    private final KnowledgeDependencyRepository knowledgeDependencyRepository;

    CourseKnowledgeControllerTest(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            CourseEnrollmentRepository courseEnrollmentRepository,
            CourseRepository courseRepository,
            KnowledgePointRepository knowledgePointRepository,
            KnowledgeDependencyRepository knowledgeDependencyRepository
    ) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
        this.courseRepository = courseRepository;
        this.knowledgePointRepository = knowledgePointRepository;
        this.knowledgeDependencyRepository = knowledgeDependencyRepository;
    }

    @Test
    void createsCourseChapterKnowledgePointsAndGraph() throws Exception {
        String courseBody = mockMvc.perform(post("/api/courses")
                        .header("X-User-Id", TEACHER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Spring Boot Foundations",
                                  "description": "Backend APIs, persistence, and testing.",
                                  "teacherId": "teacher_1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.title").value("Spring Boot Foundations"))
                .andExpect(jsonPath("$.data.teacherId").value(TEACHER_ID))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String courseId = objectMapper.readTree(courseBody).path("data").path("id").asText();
        assertThat(courseId).isNotBlank();

        String chapterBody = mockMvc.perform(post("/api/courses/{courseId}/chapters", courseId)
                        .header("X-User-Id", TEACHER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "JPA Persistence",
                                  "sequenceNo": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andExpect(jsonPath("$.data.sequenceNo").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String chapterId = objectMapper.readTree(chapterBody).path("data").path("id").asText();
        assertThat(chapterId).isNotBlank();

        String prereqBody = mockMvc.perform(post("/api/knowledge-points")
                        .header("X-User-Id", TEACHER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseId": "%s",
                                  "chapterId": "%s",
                                  "title": "Entity Mapping",
                                  "description": "Map tables to JPA entities.",
                                  "difficulty": 0.35
                                }
                                """.formatted(courseId, chapterId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.title").value("Entity Mapping"))
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String prerequisiteId = objectMapper.readTree(prereqBody).path("data").path("id").asText();

        String pointBody = mockMvc.perform(post("/api/knowledge-points")
                        .header("X-User-Id", TEACHER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseId": "%s",
                                  "chapterId": "%s",
                                  "title": "Repository Services",
                                  "description": "Persist through services and repositories.",
                                  "difficulty": 0.55
                                }
                                """.formatted(courseId, chapterId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.title").value("Repository Services"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String knowledgePointId = objectMapper.readTree(pointBody).path("data").path("id").asText();
        assertThat(prerequisiteId).isNotBlank();
        assertThat(knowledgePointId).isNotBlank();

        mockMvc.perform(post("/api/knowledge-dependencies")
                        .header("X-User-Id", TEACHER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "knowledgePointId": "%s",
                                  "prerequisiteId": "%s",
                                  "dependencyType": "PREREQUISITE"
                                }
                                """.formatted(knowledgePointId, prerequisiteId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.knowledgePointId").value(knowledgePointId))
                .andExpect(jsonPath("$.data.prerequisiteId").value(prerequisiteId))
                .andExpect(jsonPath("$.data.dependencyType").value("PREREQUISITE"));

        mockMvc.perform(get("/api/courses/{courseId}", courseId)
                        .header("X-User-Id", TEACHER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(courseId))
                .andExpect(jsonPath("$.data.title").value("Spring Boot Foundations"));

        mockMvc.perform(get("/api/courses")
                        .header("X-User-Id", TEACHER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(courseId));

        mockMvc.perform(get("/api/courses/{courseId}/knowledge-graph", courseId)
                        .header("X-User-Id", TEACHER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.course.id").value(courseId))
                .andExpect(jsonPath("$.data.chapters.length()").value(1))
                .andExpect(jsonPath("$.data.chapters[0].id").value(chapterId))
                .andExpect(jsonPath("$.data.knowledgePoints.length()").value(2))
                .andExpect(jsonPath("$.data.dependencies.length()").value(1))
                .andExpect(jsonPath("$.data.dependencies[0].knowledgePointId").value(knowledgePointId))
                .andExpect(jsonPath("$.data.dependencies[0].prerequisiteId").value(prerequisiteId));
    }

    @Test
    void rejectsUnsupportedKnowledgeDependencyType() throws Exception {
        String courseId = createCourse("Dependency Type Course");
        String chapterId = createChapter(courseId);
        String prerequisiteId = createKnowledgePoint(courseId, chapterId, "Entity Mapping", 0.35);
        String knowledgePointId = createKnowledgePoint(courseId, chapterId, "Repository Services", 0.55);

        mockMvc.perform(post("/api/knowledge-dependencies")
                        .header("X-User-Id", TEACHER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "knowledgePointId": "%s",
                                  "prerequisiteId": "%s",
                                  "dependencyType": "BLOCKING"
                                }
                                """.formatted(knowledgePointId, prerequisiteId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("dependencyType")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("PREREQUISITE")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("RELATED")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("ADVANCED")))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void rejectsStudentCreatingCourse() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .header("X-User-Id", STUDENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Forbidden Course",
                                  "description": "Student should not create courses.",
                                  "teacherId": "teacher_1"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void teacherCannotCreateCourseForAnotherTeacher() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .header("X-User-Id", TEACHER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Foreign Teacher Course",
                                  "description": "Teacher id mismatch.",
                                  "teacherId": "teacher_2"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void teacherCourseCreationUsesCurrentTeacherIdWhenTeacherIdIsOmitted() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .header("X-User-Id", TEACHER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Teacher Owned Course",
                                  "description": "Teacher id is assigned by backend."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teacherId").value(TEACHER_ID));
    }

    @Test
    void foreignTeacherCannotManageCourseGraph() throws Exception {
        String courseId = createCourse("Owned Course");
        String chapterId = createChapter(courseId);
        String prerequisiteId = createKnowledgePoint(courseId, chapterId, "Entity Mapping", 0.35);
        String knowledgePointId = createKnowledgePoint(courseId, chapterId, "Repository Services", 0.55);

        mockMvc.perform(post("/api/courses/{courseId}/chapters", courseId)
                        .header("X-User-Id", OTHER_TEACHER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Forbidden Chapter",
                                  "sequenceNo": 2
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(post("/api/knowledge-points")
                        .header("X-User-Id", OTHER_TEACHER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseId": "%s",
                                  "chapterId": "%s",
                                  "title": "Forbidden Knowledge",
                                  "description": "Foreign teacher cannot add it.",
                                  "difficulty": 0.4
                                }
                                """.formatted(courseId, chapterId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(post("/api/knowledge-dependencies")
                        .header("X-User-Id", OTHER_TEACHER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "knowledgePointId": "%s",
                                  "prerequisiteId": "%s",
                                  "dependencyType": "PREREQUISITE"
                                }
                                """.formatted(knowledgePointId, prerequisiteId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void studentCannotManageCourseGraphEvenWhenActivelyEnrolled() throws Exception {
        String courseId = createCourse("Enrolled Student Write Guard");
        String chapterId = createChapter(courseId);
        String prerequisiteId = createKnowledgePoint(courseId, chapterId, "Entity Mapping", 0.35);
        String knowledgePointId = createKnowledgePoint(courseId, chapterId, "Repository Services", 0.55);
        seedEnrollment(courseId, STUDENT_ID, "ACTIVE");

        String chapterBody = mockMvc.perform(post("/api/courses/{courseId}/chapters", courseId)
                        .header("X-User-Id", STUDENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Student Chapter Pollution",
                                  "sequenceNo": 99
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String pointBody = mockMvc.perform(post("/api/knowledge-points")
                        .header("X-User-Id", STUDENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseId": "%s",
                                  "chapterId": "%s",
                                  "title": "Student Knowledge Pollution",
                                  "description": "Students must not write graph nodes.",
                                  "difficulty": 0.4
                                }
                                """.formatted(courseId, chapterId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String dependencyBody = mockMvc.perform(post("/api/knowledge-dependencies")
                        .header("X-User-Id", STUDENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "knowledgePointId": "%s",
                                  "prerequisiteId": "%s",
                                  "dependencyType": "PREREQUISITE"
                                }
                                """.formatted(knowledgePointId, prerequisiteId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(chapterBody).doesNotContain(courseId, chapterId);
        assertThat(pointBody).doesNotContain(courseId, chapterId);
        assertThat(dependencyBody).doesNotContain(knowledgePointId, prerequisiteId);
    }

    @Test
    void knowledgePointRejectsForgedForeignChapterIdForAuthorizedWritersWithoutSideEffects() throws Exception {
        String ownCourseId = createCourseAsAdminForTeacher(INSTRUCTOR_ID, "Forged Chapter Own Course");
        String foreignCourseId = createCourseForTeacher(OTHER_TEACHER_ID, "Forged Chapter Foreign Course");
        String foreignChapterId = createChapterForUser(
                foreignCourseId,
                OTHER_TEACHER_ID,
                "Forged Foreign Chapter"
        );
        long beforeCount = knowledgePointRepository.count();

        String teacherBody = mockMvc.perform(post("/api/knowledge-points")
                        .header("Authorization", "Bearer " + jwt(INSTRUCTOR_ID, "Instructor One", List.of("TEACHER")))
                        .header("X-User-Id", ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseId": "%s",
                                  "chapterId": "%s",
                                  "title": "Forged Chapter Knowledge Pollution",
                                  "description": "Authorized writer must not splice a foreign chapter.",
                                  "difficulty": 0.4
                                }
                                """.formatted(ownCourseId, foreignChapterId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String adminBody = mockMvc.perform(post("/api/knowledge-points")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", STUDENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseId": "%s",
                                  "chapterId": "%s",
                                  "title": "Forged Chapter Admin Pollution",
                                  "description": "Admin must still respect chapter-course consistency.",
                                  "difficulty": 0.5
                                }
                                """.formatted(ownCourseId, foreignChapterId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(teacherBody)
                .doesNotContain(ownCourseId)
                .doesNotContain(foreignCourseId)
                .doesNotContain(foreignChapterId)
                .doesNotContain("Forged Foreign Chapter")
                .doesNotContain("Forged Chapter Knowledge Pollution");
        assertThat(adminBody)
                .doesNotContain(ownCourseId)
                .doesNotContain(foreignCourseId)
                .doesNotContain(foreignChapterId)
                .doesNotContain("Forged Foreign Chapter")
                .doesNotContain("Forged Chapter Admin Pollution");
        assertThat(knowledgePointRepository.count()).isEqualTo(beforeCount);
    }

    @Test
    void knowledgeDependencyRejectsCrossCourseForgedPrerequisiteWithoutSideEffects() throws Exception {
        String courseId = createCourse("Cross Course Dependency Target Course");
        String chapterId = createChapter(courseId);
        String knowledgePointId = createKnowledgePoint(courseId, chapterId, "Cross Course Target", 0.55);

        String foreignCourseId = createCourseForTeacher(OTHER_TEACHER_ID, "Cross Course Dependency Foreign Course");
        String foreignChapterId = createChapterForUser(foreignCourseId, OTHER_TEACHER_ID, "Foreign Dependency Chapter");
        String foreignPrerequisiteId = createKnowledgePointForUser(
                foreignCourseId,
                foreignChapterId,
                OTHER_TEACHER_ID,
                "Foreign Dependency Prerequisite",
                0.25
        );
        long beforeCount = knowledgeDependencyRepository.count();

        String body = mockMvc.perform(post("/api/knowledge-dependencies")
                        .header("Authorization", "Bearer " + jwt(TEACHER_ID, "Teacher One", List.of("TEACHER")))
                        .header("X-User-Id", ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "knowledgePointId": "%s",
                                  "prerequisiteId": "%s",
                                  "dependencyType": "PREREQUISITE"
                                }
                                """.formatted(knowledgePointId, foreignPrerequisiteId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .doesNotContain(courseId)
                .doesNotContain(foreignCourseId)
                .doesNotContain(knowledgePointId)
                .doesNotContain(foreignPrerequisiteId)
                .doesNotContain("Cross Course Target")
                .doesNotContain("Foreign Dependency Prerequisite");
        assertThat(knowledgeDependencyRepository.count()).isEqualTo(beforeCount);
        mockMvc.perform(get("/api/courses/{courseId}/knowledge-graph", courseId)
                        .header("Authorization", "Bearer " + jwt(TEACHER_ID, "Teacher One", List.of("TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dependencies.length()").value(0));
    }

    @Test
    void adminCanManageAnyCourseGraph() throws Exception {
        String courseId = createCourse("Admin Managed Course");

        String chapterBody = mockMvc.perform(post("/api/courses/{courseId}/chapters", courseId)
                        .header("X-User-Id", ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Admin Chapter",
                                  "sequenceNo": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String chapterId = objectMapper.readTree(chapterBody).path("data").path("id").asText();

        String pointBody = mockMvc.perform(post("/api/knowledge-points")
                        .header("X-User-Id", ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseId": "%s",
                                  "chapterId": "%s",
                                  "title": "Admin Knowledge",
                                  "description": "Admin can edit foreign course graph.",
                                  "difficulty": 0.45
                                }
                                """.formatted(courseId, chapterId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String adminPointId = objectMapper.readTree(pointBody).path("data").path("id").asText();

        String prereqId = createKnowledgePoint(courseId, chapterId, "Teacher Knowledge", 0.25);
        mockMvc.perform(post("/api/knowledge-dependencies")
                        .header("X-User-Id", ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "knowledgePointId": "%s",
                                  "prerequisiteId": "%s",
                                  "dependencyType": "RELATED"
                                }
                                """.formatted(adminPointId, prereqId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dependencyType").value("RELATED"));
    }

    @Test
    void courseListIsScopedByCurrentUserRole() throws Exception {
        String teacherCourseId = createCourseForTeacher(TEACHER_ID, "Teacher One Course");
        String otherTeacherCourseId = createCourseForTeacher(OTHER_TEACHER_ID, "Teacher Two Course");
        seedEnrollment(teacherCourseId, STUDENT_ID, "ACTIVE");
        seedEnrollment(otherTeacherCourseId, STUDENT_ID, "DROPPED");

        mockMvc.perform(get("/api/courses")
                        .header("X-User-Id", ADMIN_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/api/courses")
                        .header("X-User-Id", TEACHER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(teacherCourseId));

        mockMvc.perform(get("/api/courses")
                        .header("X-User-Id", OTHER_TEACHER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(otherTeacherCourseId));

        String studentBody = mockMvc.perform(get("/api/courses")
                        .header("X-User-Id", STUDENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(teacherCourseId))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(studentBody)
                .doesNotContain(otherTeacherCourseId)
                .doesNotContain("Teacher Two Course");
    }

    @Test
    void studentCourseReadAndGraphRequireActiveEnrollment() throws Exception {
        String activeCourseId = createCourseForTeacher(TEACHER_ID, "Active Enrollment Course");
        String droppedCourseId = createCourseForTeacher(TEACHER_ID, "Dropped Enrollment Course");
        String activeChapterId = createChapter(activeCourseId);
        createKnowledgePoint(activeCourseId, activeChapterId, "Active Course Knowledge", 0.4);
        seedEnrollment(activeCourseId, STUDENT_ID, "ACTIVE");
        seedEnrollment(droppedCourseId, STUDENT_ID, "DROPPED");

        mockMvc.perform(get("/api/courses/{courseId}", activeCourseId)
                        .header("X-User-Id", STUDENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(activeCourseId));

        mockMvc.perform(get("/api/courses/{courseId}/knowledge-graph", activeCourseId)
                        .header("X-User-Id", STUDENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.course.id").value(activeCourseId));

        mockMvc.perform(get("/api/courses/{courseId}", droppedCourseId)
                        .header("X-User-Id", STUDENT_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/api/courses/{courseId}/knowledge-graph", droppedCourseId)
                        .header("X-User-Id", STUDENT_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void nonAdminCannotDistinguishMissingCourseFromForbiddenCourse() throws Exception {
        String courseId = createCourse("Scoped Course");

        mockMvc.perform(get("/api/courses/{courseId}", courseId)
                        .header("X-User-Id", OTHER_TEACHER_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/api/courses/{courseId}", "course_missing")
                        .header("X-User-Id", OTHER_TEACHER_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void adminSeesMissingCourseAsNotFoundForDetailAndGraph() throws Exception {
        mockMvc.perform(get("/api/courses/{courseId}", "course_missing_admin_scope")
                        .header("X-User-Id", ADMIN_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/api/courses/{courseId}/knowledge-graph", "course_missing_admin_scope")
                        .header("X-User-Id", ADMIN_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void knowledgeGraphUsesCourseReadScope() throws Exception {
        String courseId = createCourse("Knowledge Graph Scope Course");
        String chapterId = createChapter(courseId);
        createKnowledgePoint(courseId, chapterId, "Scoped Knowledge", 0.4);

        mockMvc.perform(get("/api/courses/{courseId}/knowledge-graph", courseId)
                        .header("X-User-Id", TEACHER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.course.id").value(courseId));

        mockMvc.perform(get("/api/courses/{courseId}/knowledge-graph", courseId)
                        .header("X-User-Id", OTHER_TEACHER_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/api/courses/{courseId}/knowledge-graph", "course_missing")
                        .header("X-User-Id", OTHER_TEACHER_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void courseListUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader() throws Exception {
        String teacherCourseId = createCourseForTeacher(TEACHER_ID, "Bearer Admin List Course One");
        String otherTeacherCourseId = createCourseForTeacher(OTHER_TEACHER_ID, "Bearer Admin List Course Two");

        String body = mockMvc.perform(get("/api/courses")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", STUDENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .contains(teacherCourseId)
                .contains(otherTeacherCourseId);
    }

    @Test
    void courseListBearerTeacherIgnoresSpoofedAdminHeaderAndReturnsOnlyOwnedCourses() throws Exception {
        String ownedCourseId = createCourseAsAdminForTeacher(INSTRUCTOR_ID, "Bearer Teacher Owned List Course");
        String foreignCourseId = createCourseForTeacher(OTHER_TEACHER_ID, "Bearer Teacher Foreign List Course");

        String body = mockMvc.perform(get("/api/courses")
                        .header("Authorization", "Bearer " + jwt(INSTRUCTOR_ID, "Instructor One", List.of("TEACHER")))
                        .header("X-User-Id", ADMIN_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(ownedCourseId))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .contains(ownedCourseId)
                .doesNotContain(foreignCourseId)
                .doesNotContain("Bearer Teacher Foreign List Course");
    }

    @Test
    void courseListBearerStudentWithSpoofedTeacherHeaderReturnsOnlyActiveEnrollments() throws Exception {
        String activeCourseId = createCourseForTeacher(TEACHER_ID, "Bearer Student Active List Course");
        String droppedCourseId = createCourseForTeacher(OTHER_TEACHER_ID, "Bearer Student Dropped List Course");
        seedEnrollment(activeCourseId, STUDENT_ID, "ACTIVE");
        seedEnrollment(droppedCourseId, STUDENT_ID, "DROPPED");

        String body = mockMvc.perform(get("/api/courses")
                        .header("Authorization", "Bearer " + jwt(STUDENT_ID, "Alice", List.of("STUDENT")))
                        .header("X-User-Id", TEACHER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(activeCourseId))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .contains(activeCourseId)
                .doesNotContain(droppedCourseId)
                .doesNotContain("Bearer Student Dropped List Course");
    }

    @Test
    void courseDetailAndGraphUseBearerAdminRoleAndIgnoreSpoofedUserIdHeader() throws Exception {
        String courseId = createCourse("Bearer Admin Detail Course");
        String chapterId = createChapter(courseId);
        createKnowledgePoint(courseId, chapterId, "Bearer Admin Graph Knowledge", 0.4);

        mockMvc.perform(get("/api/courses/{courseId}", courseId)
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", STUDENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.id").value(courseId));

        mockMvc.perform(get("/api/courses/{courseId}/knowledge-graph", courseId)
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", STUDENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.course.id").value(courseId));
    }

    @Test
    void bearerAdminSeesMissingCourseAsNotFoundForDetailAndGraph() throws Exception {
        mockMvc.perform(get("/api/courses/{courseId}", "course_missing_roles_admin")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", STUDENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/api/courses/{courseId}/knowledge-graph", "course_missing_roles_admin")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", STUDENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void bearerTeacherRoleCanReadAndManageOwnedCourseWithoutTeacherIdPrefix() throws Exception {
        String courseId = createCourseAsAdminForTeacher(INSTRUCTOR_ID, "Bearer Instructor Course");

        mockMvc.perform(get("/api/courses/{courseId}", courseId)
                        .header("Authorization", "Bearer " + jwt(INSTRUCTOR_ID, "Instructor One", List.of("TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.id").value(courseId))
                .andExpect(jsonPath("$.data.teacherId").value(INSTRUCTOR_ID));

        mockMvc.perform(get("/api/courses/{courseId}/knowledge-graph", courseId)
                        .header("Authorization", "Bearer " + jwt(INSTRUCTOR_ID, "Instructor One", List.of("TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.course.id").value(courseId));

        mockMvc.perform(post("/api/courses/{courseId}/chapters", courseId)
                        .header("Authorization", "Bearer " + jwt(INSTRUCTOR_ID, "Instructor One", List.of("TEACHER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Bearer Teacher Chapter",
                                  "sequenceNo": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.courseId").value(courseId));
    }

    @Test
    void bearerStudentRoleWithSpoofedAdminHeaderCannotReadOrManageForeignCourse() throws Exception {
        String courseId = createCourse("Bearer Student Foreign Course");

        String detailBody = mockMvc.perform(get("/api/courses/{courseId}", courseId)
                        .header("Authorization", "Bearer " + jwt(STUDENT_ID, "Alice", List.of("STUDENT")))
                        .header("X-User-Id", ADMIN_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String graphBody = mockMvc.perform(get("/api/courses/{courseId}/knowledge-graph", courseId)
                        .header("Authorization", "Bearer " + jwt(STUDENT_ID, "Alice", List.of("STUDENT")))
                        .header("X-User-Id", ADMIN_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String chapterBody = mockMvc.perform(post("/api/courses/{courseId}/chapters", courseId)
                        .header("Authorization", "Bearer " + jwt(STUDENT_ID, "Alice", List.of("STUDENT")))
                        .header("X-User-Id", ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Student Spoofed Chapter",
                                  "sequenceNo": 9
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(detailBody).doesNotContain(courseId);
        assertThat(graphBody).doesNotContain(courseId);
        assertThat(chapterBody).doesNotContain(courseId);
    }

    @Test
    void bearerUserSubjectAdminDoesNotGainCourseAdminAccess() throws Exception {
        String courseId = createCourse("Bearer User Subject Admin Course");

        String detailBody = mockMvc.perform(get("/api/courses/{courseId}", courseId)
                        .header("Authorization", "Bearer " + jwt(ADMIN_ID, "Admin Subject Only", List.of("USER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String graphBody = mockMvc.perform(get("/api/courses/{courseId}/knowledge-graph", courseId)
                        .header("Authorization", "Bearer " + jwt(ADMIN_ID, "Admin Subject Only", List.of("USER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(detailBody).doesNotContain(courseId);
        assertThat(graphBody).doesNotContain(courseId);
    }

    @Test
    void bearerUserSubjectTeacherPrefixDoesNotGainTeacherManageAccess() throws Exception {
        String courseId = createCourse("Bearer User Subject Teacher Prefix Course");

        String body = mockMvc.perform(post("/api/courses/{courseId}/chapters", courseId)
                        .header("Authorization", "Bearer " + jwt(TEACHER_ID, "Teacher Subject Only", List.of("USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Subject Prefix Chapter",
                                  "sequenceNo": 2
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain(courseId);
    }

    @Test
    void bearerUserSubjectTeacherPrefixCannotCreateCourseForSelf() throws Exception {
        String body = mockMvc.perform(post("/api/courses")
                        .header("Authorization", "Bearer " + jwt(TEACHER_ID, "Teacher Subject Only", List.of("USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Subject Prefix Self Course",
                                  "description": "Subject prefix must not become teacher role.",
                                  "teacherId": "%s"
                                }
                                """.formatted(TEACHER_ID)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .doesNotContain("Subject Prefix Self Course")
                .doesNotContain(TEACHER_ID);
    }

    @Test
    void bearerUserSubjectAdminCannotCreateCourseAndDoesNotPersistCourse() throws Exception {
        long beforeCount = courseRepository.count();

        String body = mockMvc.perform(post("/api/courses")
                        .header("Authorization", "Bearer " + jwt(ADMIN_ID, "Admin Subject Only", List.of("USER")))
                        .header("X-User-Id", TEACHER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Subject Admin Created Course",
                                  "description": "Subject admin must not become admin role.",
                                  "teacherId": "%s"
                                }
                                """.formatted(TEACHER_ID)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .doesNotContain("Subject Admin Created Course")
                .doesNotContain(TEACHER_ID);
        assertThat(courseRepository.count()).isEqualTo(beforeCount);
    }

    @Test
    void bearerTeacherCreatesOwnCourseDespiteSpoofedAdminHeader() throws Exception {
        String body = mockMvc.perform(post("/api/courses")
                        .header("Authorization", "Bearer " + jwt(INSTRUCTOR_ID, "Instructor One", List.of("TEACHER")))
                        .header("X-User-Id", ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Bearer Teacher Spoofed Admin Course",
                                  "description": "Token teacher should remain course owner.",
                                  "teacherId": "%s"
                                }
                                """.formatted(INSTRUCTOR_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.title").value("Bearer Teacher Spoofed Admin Course"))
                .andExpect(jsonPath("$.data.teacherId").value(INSTRUCTOR_ID))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String courseId = objectMapper.readTree(body).path("data").path("id").asText();
        assertThat(courseRepository.findById(courseId))
                .hasValueSatisfying(course -> assertThat(course.getTeacherId()).isEqualTo(INSTRUCTOR_ID));
    }

    @Test
    void bearerUserSubjectTeacherPrefixCannotCreateKnowledgePointInSubjectOwnedCourse() throws Exception {
        String courseId = createCourse("Bearer User Subject Teacher Prefix Knowledge Course");
        String chapterId = createChapter(courseId);

        String body = mockMvc.perform(post("/api/knowledge-points")
                        .header("Authorization", "Bearer " + jwt(TEACHER_ID, "Teacher Subject Only", List.of("USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseId": "%s",
                                  "chapterId": "%s",
                                  "title": "Subject Prefix Knowledge",
                                  "description": "Subject prefix must not create knowledge.",
                                  "difficulty": 0.4
                                }
                                """.formatted(courseId, chapterId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .doesNotContain(courseId)
                .doesNotContain(chapterId)
                .doesNotContain("Subject Prefix Knowledge");
    }

    @Test
    void knowledgeDependencyRejectsBearerUserSubjectTeacherPrefixForSubjectOwnedCourse() throws Exception {
        String courseId = createCourse("Bearer User Subject Teacher Prefix Dependency Course");
        String chapterId = createChapter(courseId);
        String prerequisiteId = createKnowledgePoint(courseId, chapterId, "Subject Prefix Prerequisite", 0.35);
        String knowledgePointId = createKnowledgePoint(courseId, chapterId, "Subject Prefix Target", 0.55);

        String body = mockMvc.perform(post("/api/knowledge-dependencies")
                        .header("Authorization", "Bearer " + jwt(TEACHER_ID, "Teacher Subject Only", List.of("USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "knowledgePointId": "%s",
                                  "prerequisiteId": "%s",
                                  "dependencyType": "PREREQUISITE"
                                }
                                """.formatted(knowledgePointId, prerequisiteId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .doesNotContain(courseId)
                .doesNotContain(knowledgePointId)
                .doesNotContain(prerequisiteId);
    }

    private String createCourse(String title) throws Exception {
        return createCourseForTeacher(TEACHER_ID, title);
    }

    private String createCourseAsAdminForTeacher(String teacherId, String title) throws Exception {
        String body = mockMvc.perform(post("/api/courses")
                        .header("X-User-Id", ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "description": "Admin seeded course.",
                                  "teacherId": "%s"
                                }
                                """.formatted(title, teacherId)))
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
                                  "description": "Knowledge dependency validation.",
                                  "teacherId": "%s"
                                }
                                """.formatted(title, teacherId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).path("data").path("id").asText();
    }

    private String createChapter(String courseId) throws Exception {
        return createChapterForUser(courseId, TEACHER_ID, "Knowledge DAG");
    }

    private String createChapterForUser(String courseId, String userId, String title) throws Exception {
        String body = mockMvc.perform(post("/api/courses/{courseId}/chapters", courseId)
                        .header("X-User-Id", userId)
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

    private String createKnowledgePoint(String courseId, String chapterId, String title, double difficulty) throws Exception {
        return createKnowledgePointForUser(courseId, chapterId, TEACHER_ID, title, difficulty);
    }

    private String createKnowledgePointForUser(
            String courseId,
            String chapterId,
            String userId,
            String title,
            double difficulty
    ) throws Exception {
        String body = mockMvc.perform(post("/api/knowledge-points")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseId": "%s",
                                  "chapterId": "%s",
                                  "title": "%s",
                                  "description": "Knowledge point %s",
                                  "difficulty": %s
                                }
                                """.formatted(courseId, chapterId, title, title, difficulty)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).path("data").path("id").asText();
    }

    private void seedEnrollment(String courseId, String learnerId, String status) {
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setCourseId(courseId);
        enrollment.setLearnerId(learnerId);
        enrollment.setStatus(status);
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
