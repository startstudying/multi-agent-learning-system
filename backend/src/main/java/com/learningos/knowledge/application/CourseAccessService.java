package com.learningos.knowledge.application;

import com.learningos.common.api.ErrorCode;
import com.learningos.common.exception.ApiException;
import com.learningos.knowledge.domain.Course;
import com.learningos.knowledge.domain.CourseEnrollment;
import com.learningos.knowledge.repository.CourseEnrollmentRepository;
import com.learningos.knowledge.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CourseAccessService {

    private static final String ENROLLMENT_ACTIVE = "ACTIVE";

    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;

    public CourseAccessService(
            CourseRepository courseRepository,
            CourseEnrollmentRepository courseEnrollmentRepository
    ) {
        this.courseRepository = courseRepository;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
    }

    @Transactional(readOnly = true)
    public Course requireCourseRead(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String courseId
    ) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> scopedCourseMissing(currentUserAdmin));
        if (currentUserAdmin) {
            return course;
        }
        if (currentUserTeacher
                && hasText(course.getTeacherId())
                && course.getTeacherId().equals(currentUserId)) {
            return course;
        }
        if (!currentUserTeacher
                && courseEnrollmentRepository.existsByCourseIdAndLearnerIdAndStatus(
                courseId,
                currentUserId,
                ENROLLMENT_ACTIVE
        )) {
            return course;
        }
        throw new ApiException(ErrorCode.FORBIDDEN, "Course access denied");
    }

    public void requireCourseManage(
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
        throw new ApiException(ErrorCode.FORBIDDEN, "Course write access denied");
    }

    @Transactional(readOnly = true)
    public void requireLearnerEnrolledForExistingCourse(
            String currentUserId,
            boolean currentUserAdmin,
            String learnerId,
            String courseId
    ) {
        if (!isExistingCourse(courseId)) {
            return;
        }
        if (currentUserAdmin) {
            return;
        }
        if (courseEnrollmentRepository.existsByCourseIdAndLearnerIdAndStatus(
                courseId,
                learnerId,
                ENROLLMENT_ACTIVE
        )) {
            return;
        }
        throw new ApiException(ErrorCode.FORBIDDEN, "Course enrollment required");
    }

    @Transactional(readOnly = true)
    public List<String> listActiveLearnerIds(String courseId) {
        return courseEnrollmentRepository.findByCourseIdAndStatusOrderByCreatedAtAsc(courseId, ENROLLMENT_ACTIVE)
                .stream()
                .map(CourseEnrollment::getLearnerId)
                .distinct()
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> listActiveCourseIdsForLearner(String learnerId) {
        return courseEnrollmentRepository.findByLearnerIdAndStatusOrderByCreatedAtAsc(learnerId, ENROLLMENT_ACTIVE)
                .stream()
                .map(CourseEnrollment::getCourseId)
                .distinct()
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Course> listCoursesForUser(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher
    ) {
        if (currentUserAdmin) {
            return courseRepository.findAll();
        }
        if (currentUserTeacher) {
            return courseRepository.findByTeacherIdOrderByCreatedAtAsc(currentUserId);
        }
        List<String> courseIds = listActiveCourseIdsForLearner(currentUserId);
        if (courseIds.isEmpty()) {
            return List.of();
        }
        List<Course> courses = new ArrayList<>();
        courseRepository.findAllById(courseIds).forEach(courses::add);
        Map<String, Course> courseById = courses.stream()
                .collect(Collectors.toMap(
                        Course::getId,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));
        Set<String> emitted = new LinkedHashSet<>();
        return courseIds.stream()
                .filter(emitted::add)
                .map(courseById::get)
                .filter(course -> course != null)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isExistingCourse(String courseId) {
        return hasText(courseId) && courseRepository.existsById(courseId);
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
}
