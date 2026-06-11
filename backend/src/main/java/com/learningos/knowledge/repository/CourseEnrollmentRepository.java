package com.learningos.knowledge.repository;

import com.learningos.knowledge.domain.CourseEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, String> {

    boolean existsByCourseIdAndLearnerIdAndStatus(String courseId, String learnerId, String status);

    List<CourseEnrollment> findByLearnerIdAndStatusOrderByCreatedAtAsc(String learnerId, String status);

    List<CourseEnrollment> findByCourseIdAndStatusOrderByCreatedAtAsc(String courseId, String status);

    Optional<CourseEnrollment> findByCourseIdAndLearnerId(String courseId, String learnerId);
}
