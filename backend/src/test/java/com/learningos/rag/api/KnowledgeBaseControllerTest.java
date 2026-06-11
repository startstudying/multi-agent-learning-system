package com.learningos.rag.api;

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
import static org.hamcrest.Matchers.containsInAnyOrder;
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
class KnowledgeBaseControllerTest {

    private static final String AUTH_SECRET = "unit-test-secret-32-bytes-long!!";
    private static final String AUTH_ISSUER = "learning-os";

    private final MockMvc mockMvc;

    KnowledgeBaseControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void createsKnowledgeBaseAndListsOnlyAccessibleBases() throws Exception {
        mockMvc.perform(post("/api/knowledge-bases")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Java backend course",
                                  "description": "Spring and database material",
                                  "visibility": "PRIVATE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.name").value("Java backend course"))
                .andExpect(jsonPath("$.data.ownerUserId").value("alice"));

        mockMvc.perform(post("/api/knowledge-bases")
                        .header("X-User-Id", "bob")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Bob private course",
                                  "description": "Hidden material",
                                  "visibility": "PRIVATE"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/knowledge-bases").header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Java backend course"));
    }

    @Test
    void knowledgeBaseListUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader() throws Exception {
        createKnowledgeBase("teacher_a", "Bearer Admin KB One");
        createKnowledgeBase("teacher_b", "Bearer Admin KB Two");

        mockMvc.perform(get("/api/knowledge-bases")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", "student_spoof"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[*].name", containsInAnyOrder("Bearer Admin KB One", "Bearer Admin KB Two")));
    }

    @Test
    void bearerTeacherCanCreateOwnCourseBoundKnowledgeBaseWithoutTeacherIdPrefix() throws Exception {
        String instructorId = "instructor_1";
        String courseId = createCourseAsAdminForTeacher(instructorId, "Instructor KB Course");

        mockMvc.perform(post("/api/knowledge-bases")
                        .header("Authorization", "Bearer " + jwt(instructorId, "Instructor One", List.of("TEACHER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Instructor course KB",
                                  "description": "Course scoped RAG material",
                                  "visibility": "PUBLIC",
                                  "courseId": "%s"
                                }
                                """.formatted(courseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.ownerUserId").value(instructorId))
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andExpect(jsonPath("$.data.bindingStatus").value("BOUND"));
    }

    @Test
    void bearerTeacherListRedactsForeignCourseBoundKnowledgeBasesDespitePublicVisibilityAndSpoofedHeader() throws Exception {
        String instructorId = "instructor_scope";
        String ownCourseId = createCourseAsAdminForTeacher(instructorId, "Instructor Scoped KB Course");
        String foreignCourseId = createCourseAsAdminForTeacher("other_teacher_scope", "Foreign Scoped KB Course");
        String ownKbId = createCourseBoundKnowledgeBase(instructorId, ownCourseId, "Instructor visible KB", "PUBLIC");
        String foreignKbId = createCourseBoundKnowledgeBase(
                "other_teacher_scope",
                foreignCourseId,
                "Foreign public course KB",
                "PUBLIC"
        );

        String body = mockMvc.perform(get("/api/knowledge-bases")
                        .header("Authorization", "Bearer " + jwt(instructorId, "Instructor Scope", List.of("TEACHER")))
                        .header("X-User-Id", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(ownKbId))
                .andExpect(jsonPath("$.data[0].courseId").value(ownCourseId))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .doesNotContain(foreignKbId)
                .doesNotContain(foreignCourseId)
                .doesNotContain("Foreign public course KB");
    }

    @Test
    void bearerUserSubjectTeacherPrefixCannotCreateCourseBoundKnowledgeBase() throws Exception {
        String teacherId = "teacher_1";
        String courseId = createCourseAsAdminForTeacher(teacherId, "Subject Prefix Course");

        String body = mockMvc.perform(post("/api/knowledge-bases")
                        .header("Authorization", "Bearer " + jwt(teacherId, "Subject Teacher", List.of("USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Forbidden course KB",
                                  "description": "Subject prefix is not a role",
                                  "visibility": "PRIVATE",
                                  "courseId": "%s"
                                }
                                """.formatted(courseId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        org.assertj.core.api.Assertions.assertThat(body).doesNotContain(courseId);
    }

    private String createCourseBoundKnowledgeBase(
            String userId,
            String courseId,
            String name,
            String visibility
    ) throws Exception {
        String body = mockMvc.perform(post("/api/knowledge-bases")
                        .header("Authorization", "Bearer " + jwt(userId, userId, List.of("TEACHER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "description": "Course-bound KB list scope.",
                                  "visibility": "%s",
                                  "courseId": "%s"
                                }
                                """.formatted(name, visibility, courseId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(body)
                .path("data")
                .path("id")
                .asText();
    }

    private void createKnowledgeBase(String userId, String name) throws Exception {
        mockMvc.perform(post("/api/knowledge-bases")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "description": "Private test knowledge base",
                                  "visibility": "PRIVATE"
                                }
                                """.formatted(name)))
                .andExpect(status().isOk());
    }

    private String createCourseAsAdminForTeacher(String teacherId, String title) throws Exception {
        String body = mockMvc.perform(post("/api/courses")
                        .header("X-User-Id", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "description": "Course-bound KB test course.",
                                  "teacherId": "%s"
                                }
                                """.formatted(title, teacherId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(body)
                .path("data")
                .path("id")
                .asText();
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
