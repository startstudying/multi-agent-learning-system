package com.learningos.rag.application;

import com.learningos.knowledge.domain.Course;
import com.learningos.knowledge.domain.CourseEnrollment;
import com.learningos.knowledge.repository.CourseEnrollmentRepository;
import com.learningos.knowledge.repository.CourseRepository;
import com.learningos.rag.domain.KbPermission;
import com.learningos.rag.domain.KnowledgeBase;
import com.learningos.rag.domain.enums.KnowledgeBaseBindingStatus;
import com.learningos.rag.domain.enums.Visibility;
import com.learningos.rag.repository.KbPermissionRepository;
import com.learningos.rag.repository.KnowledgeBaseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class PermissionServiceTest {

    private final PermissionService permissionService;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KbPermissionRepository permissionRepository;
    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;

    PermissionServiceTest(
            PermissionService permissionService,
            KnowledgeBaseRepository knowledgeBaseRepository,
            KbPermissionRepository permissionRepository,
            CourseRepository courseRepository,
            CourseEnrollmentRepository courseEnrollmentRepository
    ) {
        this.permissionService = permissionService;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.permissionRepository = permissionRepository;
        this.courseRepository = courseRepository;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
    }

    @Test
    void filtersRequestedKnowledgeBasesByOwnerAndVisibility() {
        KnowledgeBase owned = kb("kb_owned", "Owned", "alice", Visibility.PRIVATE);
        KnowledgeBase publicKb = kb("kb_public", "Public", "bob", Visibility.PUBLIC);
        KnowledgeBase privateOther = kb("kb_private_other", "Other", "bob", Visibility.PRIVATE);
        knowledgeBaseRepository.saveAll(List.of(owned, publicKb, privateOther));

        List<String> allowed = permissionService.filterAllowedKbIds(
                "alice",
                List.of("kb_owned", "kb_public", "kb_private_other")
        );

        assertThat(allowed).containsExactlyInAnyOrder("kb_owned", "kb_public");
    }

    @Test
    void includesExplicitUserPermissionsWhenListingAndFiltering() {
        KnowledgeBase shared = kb("kb_shared", "Shared", "bob", Visibility.PRIVATE);
        knowledgeBaseRepository.save(shared);
        permissionRepository.save(permission("kb_shared", "alice", "READ"));

        List<KnowledgeBase> accessible = permissionService.listAccessibleKnowledgeBases("alice");
        List<String> allowed = permissionService.filterAllowedKbIds("alice", List.of("kb_shared"));

        assertThat(accessible).extracting(KnowledgeBase::getId).contains("kb_shared");
        assertThat(allowed).containsExactly("kb_shared");
    }

    @Test
    void separatesReadAndWriteAccess() {
        KnowledgeBase owned = kb("kb_owned", "Owned", "alice", Visibility.PRIVATE);
        KnowledgeBase publicKb = kb("kb_public", "Public", "bob", Visibility.PUBLIC);
        KnowledgeBase shared = kb("kb_shared", "Shared", "bob", Visibility.PRIVATE);
        knowledgeBaseRepository.saveAll(List.of(owned, publicKb, shared));
        permissionRepository.save(permission("kb_shared", "alice", "WRITE"));

        assertThat(permissionService.canReadKnowledgeBase("alice", "kb_public")).isTrue();
        assertThat(permissionService.canWriteKnowledgeBase("alice", "kb_public")).isFalse();
        assertThat(permissionService.canWriteKnowledgeBase("alice", "kb_shared")).isTrue();
        assertThat(permissionService.canWriteKnowledgeBase("alice", "kb_owned")).isTrue();
    }

    @Test
    void adminRoleCanReadAndWriteAllActiveKnowledgeBasesWithoutSubjectNameInference() {
        KnowledgeBase privateOther = kb("kb_private_other", "Other", "bob", Visibility.PRIVATE);
        KnowledgeBase deleted = kb("kb_deleted", "Deleted", "bob", Visibility.PRIVATE);
        deleted.setDeletedAt(Instant.now());
        knowledgeBaseRepository.saveAll(List.of(privateOther, deleted));

        assertThat(permissionService.listAccessibleKnowledgeBases("ops_admin", true, false))
                .extracting(KnowledgeBase::getId)
                .containsExactly("kb_private_other");
        assertThat(permissionService.canReadKnowledgeBase("ops_admin", true, false, "kb_private_other")).isTrue();
        assertThat(permissionService.canWriteKnowledgeBase("ops_admin", true, false, "kb_private_other")).isTrue();
        assertThat(permissionService.canReadKnowledgeBase("ops_admin", true, false, "kb_deleted")).isFalse();
        assertThat(permissionService.canWriteKnowledgeBase("ops_admin", true, false, "kb_deleted")).isFalse();
        assertThat(permissionService.canReadKnowledgeBase("admin", false, false, "kb_private_other")).isFalse();
    }

    @Test
    void allowsActiveEnrolledStudentToReadCourseBoundPrivateKnowledgeBase() {
        course("course_sql", "instructor_1");
        enroll("course_sql", "student_active", "ACTIVE");
        KnowledgeBase bound = kb("kb_course_sql", "Course SQL", "instructor_1", Visibility.PRIVATE);
        bind(bound, "course_sql");
        knowledgeBaseRepository.save(bound);

        assertThat(permissionService.canReadKnowledgeBase("student_active", false, false, "kb_course_sql")).isTrue();
    }

    @Test
    void deniesDroppedStudentFromReadingCourseBoundPrivateKnowledgeBase() {
        course("course_sql", "instructor_1");
        enroll("course_sql", "student_dropped", "DROPPED");
        KnowledgeBase bound = kb("kb_course_sql", "Course SQL", "instructor_1", Visibility.PUBLIC);
        bind(bound, "course_sql");
        knowledgeBaseRepository.save(bound);

        assertThat(permissionService.canReadKnowledgeBase("student_dropped", false, false, "kb_course_sql")).isFalse();
    }

    @Test
    void allowsTeacherToReadAndWriteOwnCourseBoundKnowledgeBaseWithoutOwnerPermission() {
        course("course_sql", "instructor_1");
        KnowledgeBase bound = kb("kb_course_sql", "Course SQL", "other_owner", Visibility.PRIVATE);
        bind(bound, "course_sql");
        knowledgeBaseRepository.save(bound);

        assertThat(permissionService.canReadKnowledgeBase("instructor_1", false, true, "kb_course_sql")).isTrue();
        assertThat(permissionService.canWriteKnowledgeBase("instructor_1", false, true, "kb_course_sql")).isTrue();
        assertThat(permissionService.canReadKnowledgeBase("teacher_1", false, false, "kb_course_sql")).isFalse();
        assertThat(permissionService.canWriteKnowledgeBase("teacher_1", false, false, "kb_course_sql")).isFalse();
    }

    @Test
    void adminCourseBoundKnowledgeBaseStillRequiresCourseAccessPath() {
        KnowledgeBase bound = kb("kb_missing_course", "Missing course", "ops_admin", Visibility.PUBLIC);
        bind(bound, "missing_course");
        knowledgeBaseRepository.save(bound);

        assertThat(permissionService.canReadKnowledgeBase("ops_admin", true, false, "kb_missing_course")).isFalse();
        assertThat(permissionService.canWriteKnowledgeBase("ops_admin", true, false, "kb_missing_course")).isFalse();
    }

    @Test
    void conflictedKnowledgeBaseDoesNotUseOwnerPublicOrCourseDerivedRead() {
        course("course_sql", "instructor_1");
        enroll("course_sql", "student_active", "ACTIVE");
        KnowledgeBase conflicted = kb("kb_conflicted", "Conflicted", "student_active", Visibility.PUBLIC);
        conflicted.setCourseId("course_sql");
        conflicted.setBindingStatus(KnowledgeBaseBindingStatus.CONFLICTED);
        knowledgeBaseRepository.save(conflicted);

        assertThat(permissionService.canReadKnowledgeBase("student_active", false, false, "kb_conflicted")).isFalse();
        assertThat(permissionService.canReadKnowledgeBase("ops_admin", true, false, "kb_conflicted")).isTrue();
    }

    private KnowledgeBase kb(String id, String name, String ownerUserId, Visibility visibility) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(id);
        kb.setName(name);
        kb.setDescription(name + " description");
        kb.setVisibility(visibility);
        kb.setOwnerUserId(ownerUserId);
        kb.setCreatedBy(ownerUserId);
        kb.setCreatedAt(Instant.now());
        kb.setUpdatedAt(Instant.now());
        return kb;
    }

    private void bind(KnowledgeBase kb, String courseId) {
        kb.setCourseId(courseId);
        kb.setBindingStatus(KnowledgeBaseBindingStatus.BOUND);
        kb.setBoundBy(kb.getOwnerUserId());
        kb.setBoundAt(Instant.now());
    }

    private void course(String courseId, String teacherId) {
        Course course = new Course();
        course.setId(courseId);
        course.setTitle(courseId);
        course.setDescription(courseId + " description");
        course.setTeacherId(teacherId);
        courseRepository.saveAndFlush(course);
    }

    private void enroll(String courseId, String learnerId, String status) {
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setCourseId(courseId);
        enrollment.setLearnerId(learnerId);
        enrollment.setStatus(status);
        courseEnrollmentRepository.saveAndFlush(enrollment);
    }

    private KbPermission permission(String kbId, String userId, String permission) {
        KbPermission kbPermission = new KbPermission();
        kbPermission.setKbId(kbId);
        kbPermission.setSubjectType("USER");
        kbPermission.setSubjectId(userId);
        kbPermission.setPermission(permission);
        return kbPermission;
    }
}
