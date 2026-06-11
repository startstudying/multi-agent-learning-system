package com.learningos.knowledge.api;

import com.learningos.common.api.ApiResponse;
import com.learningos.common.auth.CurrentUserService;
import com.learningos.common.auth.UserContext;
import com.learningos.knowledge.application.KnowledgeCatalogService;
import com.learningos.knowledge.dto.KnowledgeDtos.ChapterResponse;
import com.learningos.knowledge.dto.KnowledgeDtos.CourseResponse;
import com.learningos.knowledge.dto.KnowledgeDtos.CreateChapterRequest;
import com.learningos.knowledge.dto.KnowledgeDtos.CreateCourseRequest;
import com.learningos.knowledge.dto.KnowledgeDtos.KnowledgeGraphResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final KnowledgeCatalogService knowledgeCatalogService;
    private final CurrentUserService currentUserService;

    public CourseController(KnowledgeCatalogService knowledgeCatalogService, CurrentUserService currentUserService) {
        this.knowledgeCatalogService = knowledgeCatalogService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ApiResponse<CourseResponse> create(@Valid @RequestBody CreateCourseRequest request) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(CourseResponse.from(
                knowledgeCatalogService.createCourse(
                        currentUser.userId(),
                        hasRole(currentUser, "ADMIN"),
                        hasRole(currentUser, "TEACHER"),
                        request
                )
        ));
    }

    @GetMapping("/{courseId}")
    public ApiResponse<CourseResponse> get(@PathVariable String courseId) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(CourseResponse.from(
                knowledgeCatalogService.getCourseForUser(
                        currentUser.userId(),
                        hasRole(currentUser, "ADMIN"),
                        hasRole(currentUser, "TEACHER"),
                        courseId
                )
        ));
    }

    @GetMapping
    public ApiResponse<List<CourseResponse>> list() {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(knowledgeCatalogService.listCoursesForUser(
                        currentUser.userId(),
                        hasRole(currentUser, "ADMIN"),
                        hasRole(currentUser, "TEACHER")
                )
                .stream()
                .map(CourseResponse::from)
                .toList());
    }

    @PostMapping("/{courseId}/chapters")
    public ApiResponse<ChapterResponse> createChapter(
            @PathVariable String courseId,
            @Valid @RequestBody CreateChapterRequest request
    ) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(ChapterResponse.from(
                knowledgeCatalogService.createChapter(
                        currentUser.userId(),
                        hasRole(currentUser, "ADMIN"),
                        hasRole(currentUser, "TEACHER"),
                        courseId,
                        request
                )
        ));
    }

    @GetMapping("/{courseId}/knowledge-graph")
    public ApiResponse<KnowledgeGraphResponse> getKnowledgeGraph(@PathVariable String courseId) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(knowledgeCatalogService.getKnowledgeGraphForUser(
                currentUser.userId(),
                hasRole(currentUser, "ADMIN"),
                hasRole(currentUser, "TEACHER"),
                courseId
        ));
    }

    private boolean hasRole(UserContext currentUser, String role) {
        return currentUser.roles().stream().anyMatch(existingRole -> role.equalsIgnoreCase(existingRole));
    }
}
