package com.learningos.knowledge.repository;

import com.learningos.knowledge.domain.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChapterRepository extends JpaRepository<Chapter, String> {

    List<Chapter> findByCourseIdOrderBySequenceNoAsc(String courseId);
}
