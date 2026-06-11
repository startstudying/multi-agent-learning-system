package com.learningos.knowledge.application;

import com.learningos.common.api.ErrorCode;
import com.learningos.common.exception.ApiException;
import com.learningos.knowledge.domain.Chapter;
import com.learningos.knowledge.domain.Course;
import com.learningos.knowledge.domain.KnowledgeDependency;
import com.learningos.knowledge.domain.KnowledgePoint;
import com.learningos.knowledge.dto.KnowledgeDtos.CreateChapterRequest;
import com.learningos.knowledge.dto.KnowledgeDtos.CreateCourseRequest;
import com.learningos.knowledge.dto.KnowledgeDtos.CreateKnowledgeDependencyRequest;
import com.learningos.knowledge.dto.KnowledgeDtos.CreateKnowledgePointRequest;
import com.learningos.knowledge.dto.KnowledgeDtos.KnowledgeGraphResponse;
import com.learningos.knowledge.repository.ChapterRepository;
import com.learningos.knowledge.repository.CourseRepository;
import com.learningos.knowledge.repository.KnowledgeDependencyRepository;
import com.learningos.knowledge.repository.KnowledgePointRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.learningos.knowledge.dto.KnowledgeDtos.ChapterResponse;
import static com.learningos.knowledge.dto.KnowledgeDtos.CourseResponse;
import static com.learningos.knowledge.dto.KnowledgeDtos.KnowledgeDependencyResponse;
import static com.learningos.knowledge.dto.KnowledgeDtos.KnowledgePointResponse;

@Service
public class KnowledgeCatalogService {

    private static final Set<String> SUPPORTED_DEPENDENCY_TYPES = Set.of(
            "PREREQUISITE",
            "RELATED",
            "ADVANCED"
    );

    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final KnowledgePointRepository knowledgePointRepository;
    private final KnowledgeDependencyRepository dependencyRepository;
    private final CourseAccessService courseAccessService;

    @Autowired
    public KnowledgeCatalogService(
            CourseRepository courseRepository,
            ChapterRepository chapterRepository,
            KnowledgePointRepository knowledgePointRepository,
            KnowledgeDependencyRepository dependencyRepository,
            CourseAccessService courseAccessService
    ) {
        this.courseRepository = courseRepository;
        this.chapterRepository = chapterRepository;
        this.knowledgePointRepository = knowledgePointRepository;
        this.dependencyRepository = dependencyRepository;
        this.courseAccessService = courseAccessService;
    }

    public KnowledgeCatalogService(
            CourseRepository courseRepository,
            ChapterRepository chapterRepository,
            KnowledgePointRepository knowledgePointRepository,
            KnowledgeDependencyRepository dependencyRepository
    ) {
        this(courseRepository, chapterRepository, knowledgePointRepository, dependencyRepository, null);
    }

    @Transactional
    public Course createCourse(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            CreateCourseRequest request
    ) {
        String ownerTeacherId = resolveCourseTeacherId(
                currentUserId,
                currentUserAdmin,
                currentUserTeacher,
                request.teacherId()
        );
        Course course = new Course();
        course.setTitle(request.title());
        course.setDescription(request.description());
        course.setTeacherId(ownerTeacherId);
        course.setStatus("DRAFT");
        return courseRepository.save(course);
    }

    @Transactional(readOnly = true)
    public Course getCourse(String courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Course not found"));
    }

    @Transactional(readOnly = true)
    public List<Course> listCourses() {
        return courseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Course getCourseForUser(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String courseId
    ) {
        if (courseAccessService != null) {
            return courseAccessService.requireCourseRead(
                    currentUserId,
                    currentUserAdmin,
                    currentUserTeacher,
                    courseId
            );
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> scopedCourseMissing(currentUserAdmin));
        requireCourseReadAccess(currentUserId, currentUserAdmin, currentUserTeacher, course);
        return course;
    }

    @Transactional(readOnly = true)
    public List<Course> listCoursesForUser(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher
    ) {
        if (courseAccessService != null) {
            return courseAccessService.listCoursesForUser(currentUserId, currentUserAdmin, currentUserTeacher);
        }
        if (currentUserAdmin) {
            return courseRepository.findAll();
        }
        if (currentUserTeacher) {
            return courseRepository.findByTeacherIdOrderByCreatedAtAsc(currentUserId);
        }
        return List.of();
    }

    @Transactional
    public Chapter createChapter(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String courseId,
            CreateChapterRequest request
    ) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> scopedCourseMissing(currentUserAdmin));
        requireCourseManageAccess(currentUserId, currentUserAdmin, currentUserTeacher, course);
        Chapter chapter = new Chapter();
        chapter.setCourseId(course.getId());
        chapter.setTitle(request.title());
        chapter.setSequenceNo(request.sequenceNo());
        return chapterRepository.save(chapter);
    }

    @Transactional
    public KnowledgePoint createKnowledgePoint(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            CreateKnowledgePointRequest request
    ) {
        Course course = validateCourseAndChapter(currentUserAdmin, request.courseId(), request.chapterId());
        requireCourseManageAccess(currentUserId, currentUserAdmin, currentUserTeacher, course);
        KnowledgePoint point = new KnowledgePoint();
        point.setCourseId(request.courseId());
        point.setChapterId(request.chapterId());
        point.setTitle(request.title());
        point.setDescription(request.description());
        point.setDifficulty(request.difficulty());
        return knowledgePointRepository.save(point);
    }

    @Transactional
    public KnowledgeDependency createDependency(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            CreateKnowledgeDependencyRequest request
    ) {
        if (request.knowledgePointId().equals(request.prerequisiteId())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Knowledge point cannot depend on itself");
        }
        KnowledgePoint point = knowledgePointRepository.findById(request.knowledgePointId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Knowledge point not found"));
        KnowledgePoint prerequisite = knowledgePointRepository.findById(request.prerequisiteId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Prerequisite knowledge point not found"));
        if (point.getCourseId() != null && prerequisite.getCourseId() != null
                && !point.getCourseId().equals(prerequisite.getCourseId())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Knowledge dependency must stay within one course");
        }
        Course course = courseForDependency(currentUserAdmin, point, prerequisite);
        requireCourseManageAccess(currentUserId, currentUserAdmin, currentUserTeacher, course);
        String dependencyType = normalizeDependencyType(request.dependencyType());

        KnowledgeDependency dependency = new KnowledgeDependency();
        dependency.setKnowledgePointId(request.knowledgePointId());
        dependency.setPrerequisiteId(request.prerequisiteId());
        dependency.setDependencyType(dependencyType);
        return dependencyRepository.save(dependency);
    }

    @Transactional(readOnly = true)
    public KnowledgeGraphResponse getKnowledgeGraph(String courseId) {
        Course course = getCourse(courseId);
        return buildKnowledgeGraph(course);
    }

    @Transactional(readOnly = true)
    public KnowledgeGraphResponse getKnowledgeGraphForUser(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String courseId
    ) {
        Course course = getCourseForUser(currentUserId, currentUserAdmin, currentUserTeacher, courseId);
        return buildKnowledgeGraph(course);
    }

    private KnowledgeGraphResponse buildKnowledgeGraph(Course course) {
        String courseId = course.getId();
        List<ChapterResponse> chapters = chapterRepository.findByCourseIdOrderBySequenceNoAsc(courseId)
                .stream()
                .map(ChapterResponse::from)
                .toList();
        List<KnowledgePoint> points = knowledgePointRepository.findByCourseIdOrderByCreatedAtAsc(courseId);
        List<String> pointIds = points.stream().map(KnowledgePoint::getId).toList();
        List<KnowledgeDependencyResponse> dependencies = pointIds.isEmpty()
                ? List.of()
                : dependencyRepository.findByKnowledgePointIdInOrPrerequisiteIdIn(pointIds, pointIds)
                .stream()
                .filter(dependency -> pointIds.contains(dependency.getKnowledgePointId())
                        && pointIds.contains(dependency.getPrerequisiteId()))
                .map(KnowledgeDependencyResponse::from)
                .toList();
        return new KnowledgeGraphResponse(
                CourseResponse.from(course),
                chapters,
                points.stream().map(KnowledgePointResponse::from).toList(),
                dependencies
        );
    }

    private Course validateCourseAndChapter(boolean currentUserAdmin, String courseId, String chapterId) {
        Course course = null;
        if (courseId != null && !courseId.isBlank()) {
            course = getCourseForScope(currentUserAdmin, courseId);
        }
        if (chapterId != null && !chapterId.isBlank()) {
            Chapter chapter = chapterRepository.findById(chapterId)
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Chapter not found"));
            if (courseId != null && !courseId.isBlank() && !courseId.equals(chapter.getCourseId())) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "Chapter does not belong to course");
            }
            if ((courseId == null || courseId.isBlank()) && chapter.getCourseId() != null) {
                course = getCourseForScope(currentUserAdmin, chapter.getCourseId());
            }
        }
        return course;
    }

    private String resolveCourseTeacherId(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String requestedTeacherId
    ) {
        if (currentUserAdmin) {
            return hasText(requestedTeacherId) ? requestedTeacherId.trim() : currentUserId;
        }
        if (!currentUserTeacher) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Course write access denied");
        }
        if (hasText(requestedTeacherId) && !currentUserId.equals(requestedTeacherId.trim())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Course write access denied");
        }
        return currentUserId;
    }

    private Course courseForDependency(boolean currentUserAdmin, KnowledgePoint point, KnowledgePoint prerequisite) {
        String courseId = hasText(point.getCourseId()) ? point.getCourseId() : prerequisite.getCourseId();
        if (!hasText(courseId)) {
            return null;
        }
        return getCourseForScope(currentUserAdmin, courseId);
    }

    private Course getCourseForScope(boolean currentUserAdmin, String courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> scopedCourseMissing(currentUserAdmin));
    }

    private void requireCourseTeacherOrAdmin(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            Course course
    ) {
        if (currentUserAdmin) {
            return;
        }
        if (!currentUserTeacher
                || course == null
                || !hasText(course.getTeacherId())
                || !course.getTeacherId().equals(currentUserId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Course write access denied");
        }
    }

    private void requireCourseManageAccess(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            Course course
    ) {
        if (courseAccessService != null) {
            courseAccessService.requireCourseManage(currentUserId, currentUserAdmin, currentUserTeacher, course);
            return;
        }
        requireCourseTeacherOrAdmin(currentUserId, currentUserAdmin, currentUserTeacher, course);
    }

    private void requireCourseReadAccess(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            Course course
    ) {
        if (currentUserAdmin) {
            return;
        }
        if (currentUserTeacher
                && course != null
                && hasText(course.getTeacherId())
                && course.getTeacherId().equals(currentUserId)) {
            return;
        }
        throw new ApiException(ErrorCode.FORBIDDEN, "Course access denied");
    }

    private ApiException scopedCourseMissing(boolean currentUserAdmin) {
        if (currentUserAdmin) {
            return new ApiException(ErrorCode.NOT_FOUND, "Course not found");
        }
        return new ApiException(ErrorCode.FORBIDDEN, "Course access denied");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizeDependencyType(String dependencyType) {
        String normalized = dependencyType.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_DEPENDENCY_TYPES.contains(normalized)) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "dependencyType must be one of: PREREQUISITE, RELATED, ADVANCED"
            );
        }
        return normalized;
    }
}
