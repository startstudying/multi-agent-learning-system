package com.learningos.rag.api;

import com.learningos.common.api.ApiResponse;
import com.learningos.common.auth.CurrentUserService;
import com.learningos.common.auth.UserContext;
import com.learningos.rag.api.dto.KnowledgeBaseDtos.CreateKnowledgeBaseRequest;
import com.learningos.rag.api.dto.KnowledgeBaseDtos.KnowledgeBaseResponse;
import com.learningos.rag.application.KnowledgeBaseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-bases")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final CurrentUserService currentUserService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService, CurrentUserService currentUserService) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ApiResponse<KnowledgeBaseResponse> create(@Valid @RequestBody CreateKnowledgeBaseRequest request) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(KnowledgeBaseResponse.from(
                knowledgeBaseService.create(
                        currentUser.userId(),
                        hasRole(currentUser, "ADMIN"),
                        hasRole(currentUser, "TEACHER"),
                        request
                )
        ));
    }

    @GetMapping
    public ApiResponse<List<KnowledgeBaseResponse>> list() {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(knowledgeBaseService.listAccessible(
                        currentUser.userId(),
                        hasRole(currentUser, "ADMIN"),
                        hasRole(currentUser, "TEACHER")
                )
                .stream()
                .map(KnowledgeBaseResponse::from)
                .toList());
    }

    private boolean hasRole(UserContext currentUser, String role) {
        return currentUser.roles().stream().anyMatch(existingRole -> role.equalsIgnoreCase(existingRole));
    }
}
