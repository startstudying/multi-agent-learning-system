package com.learningos.knowledge.repository;

import com.learningos.knowledge.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, String> {

    boolean existsByTeacherId(String teacherId);

    List<Course> findByTeacherIdOrderByCreatedAtAsc(String teacherId);
}
