package com.learningos.knowledge.dto;

import com.learningos.knowledge.domain.Chapter;
import com.learningos.knowledge.domain.Course;
import com.learningos.knowledge.domain.KnowledgeDependency;
import com.learningos.knowledge.domain.KnowledgePoint;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class KnowledgeDtos {

    private KnowledgeDtos() {
    }

    public record CreateCourseRequest(
            @NotBlank @Size(max = 255) String title,
            String description,
            @Size(max = 120) String teacherId
    ) {
    }

    public record CreateChapterRequest(
            @NotBlank @Size(max = 255) String title,
            @NotNull @Positive Integer sequenceNo
    ) {
    }

    public record CreateKnowledgePointRequest(
            String courseId,
            String chapterId,
            @NotBlank @Size(max = 255) String title,
            String description,
            @DecimalMin("0.0") @DecimalMax("1.0") Double difficulty
    ) {
    }

    public record CreateKnowledgeDependencyRequest(
            @NotBlank String knowledgePointId,
            @NotBlank String prerequisiteId,
            @NotBlank @Size(max = 40) String dependencyType
    ) {
    }

    public record CourseResponse(
            String id,
            String title,
            String description,
            String teacherId,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static CourseResponse from(Course course) {
            return new CourseResponse(
                    course.getId(),
                    course.getTitle(),
                    course.getDescription(),
                    course.getTeacherId(),
                    course.getStatus(),
                    course.getCreatedAt(),
                    course.getUpdatedAt()
            );
        }
    }

    public record ChapterResponse(
            String id,
            String courseId,
            String title,
            Integer sequenceNo,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static ChapterResponse from(Chapter chapter) {
            return new ChapterResponse(
                    chapter.getId(),
                    chapter.getCourseId(),
                    chapter.getTitle(),
                    chapter.getSequenceNo(),
                    chapter.getCreatedAt(),
                    chapter.getUpdatedAt()
            );
        }
    }

    public record KnowledgePointResponse(
            String id,
            String courseId,
            String chapterId,
            String title,
            String description,
            Double difficulty,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static KnowledgePointResponse from(KnowledgePoint knowledgePoint) {
            return new KnowledgePointResponse(
                    knowledgePoint.getId(),
                    knowledgePoint.getCourseId(),
                    knowledgePoint.getChapterId(),
                    knowledgePoint.getTitle(),
                    knowledgePoint.getDescription(),
                    knowledgePoint.getDifficulty(),
                    knowledgePoint.getCreatedAt(),
                    knowledgePoint.getUpdatedAt()
            );
        }
    }

    public record KnowledgeDependencyResponse(
            String id,
            String knowledgePointId,
            String prerequisiteId,
            String dependencyType,
            Instant createdAt
    ) {
        public static KnowledgeDependencyResponse from(KnowledgeDependency dependency) {
            return new KnowledgeDependencyResponse(
                    dependency.getId(),
                    dependency.getKnowledgePointId(),
                    dependency.getPrerequisiteId(),
                    dependency.getDependencyType(),
                    dependency.getCreatedAt()
            );
        }
    }

    public record KnowledgeGraphResponse(
            CourseResponse course,
            List<ChapterResponse> chapters,
            List<KnowledgePointResponse> knowledgePoints,
            List<KnowledgeDependencyResponse> dependencies
    ) {
    }
}
