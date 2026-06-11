package com.learningos.knowledge.api;

import com.learningos.common.api.ApiResponse;
import com.learningos.common.auth.CurrentUserService;
import com.learningos.common.auth.UserContext;
import com.learningos.knowledge.application.KnowledgeCatalogService;
import com.learningos.knowledge.dto.KnowledgeDtos.CreateKnowledgeDependencyRequest;
import com.learningos.knowledge.dto.KnowledgeDtos.CreateKnowledgePointRequest;
import com.learningos.knowledge.dto.KnowledgeDtos.KnowledgeDependencyResponse;
import com.learningos.knowledge.dto.KnowledgeDtos.KnowledgePointResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KnowledgePointController {

    private final KnowledgeCatalogService knowledgeCatalogService;
    private final CurrentUserService currentUserService;

    public KnowledgePointController(KnowledgeCatalogService knowledgeCatalogService, CurrentUserService currentUserService) {
        this.knowledgeCatalogService = knowledgeCatalogService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/api/knowledge-points")
    public ApiResponse<KnowledgePointResponse> createKnowledgePoint(
            @Valid @RequestBody CreateKnowledgePointRequest request
    ) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(KnowledgePointResponse.from(
                knowledgeCatalogService.createKnowledgePoint(
                        currentUser.userId(),
                        hasRole(currentUser, "ADMIN"),
                        hasRole(currentUser, "TEACHER"),
                        request
                )
        ));
    }

    @PostMapping("/api/knowledge-dependencies")
    public ApiResponse<KnowledgeDependencyResponse> createDependency(
            @Valid @RequestBody CreateKnowledgeDependencyRequest request
    ) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(KnowledgeDependencyResponse.from(
                knowledgeCatalogService.createDependency(
                        currentUser.userId(),
                        hasRole(currentUser, "ADMIN"),
                        hasRole(currentUser, "TEACHER"),
                        request
                )
        ));
    }

    private boolean hasRole(UserContext currentUser, String role) {
        return currentUser.roles().stream().anyMatch(existingRole -> role.equalsIgnoreCase(existingRole));
    }
}
