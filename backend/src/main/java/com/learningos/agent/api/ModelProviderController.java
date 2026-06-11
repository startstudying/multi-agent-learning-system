package com.learningos.agent.api;

import com.learningos.agent.application.ModelProviderService;
import com.learningos.agent.dto.ModelProviderResponse;
import com.learningos.agent.dto.ModelProviderTestConnectionResponse;
import com.learningos.agent.dto.ModelProviderUpsertRequest;
import com.learningos.common.api.ApiResponse;
import com.learningos.common.auth.CurrentUserService;
import com.learningos.common.auth.UserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/model-providers")
public class ModelProviderController {

    private final ModelProviderService modelProviderService;
    private final CurrentUserService currentUserService;

    public ModelProviderController(ModelProviderService modelProviderService, CurrentUserService currentUserService) {
        this.modelProviderService = modelProviderService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ApiResponse<List<ModelProviderResponse>> list() {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(modelProviderService.list(isAdmin(currentUser)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ModelProviderResponse> get(@PathVariable String id) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(modelProviderService.get(id, isAdmin(currentUser)));
    }

    @PostMapping
    public ApiResponse<ModelProviderResponse> create(@Valid @RequestBody ModelProviderUpsertRequest request) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(modelProviderService.create(request, isAdmin(currentUser), currentUser.userId()));
    }

    @PutMapping("/{id}")
    public ApiResponse<ModelProviderResponse> update(
            @PathVariable String id,
            @Valid @RequestBody ModelProviderUpsertRequest request
    ) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(modelProviderService.update(id, request, isAdmin(currentUser)));
    }

    @PostMapping("/{id}/set-default")
    public ApiResponse<ModelProviderResponse> setDefault(@PathVariable String id) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(modelProviderService.setDefault(id, isAdmin(currentUser)));
    }

    @PostMapping("/{id}/test-connection")
    public ApiResponse<ModelProviderTestConnectionResponse> testConnection(@PathVariable String id) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(modelProviderService.testConnection(id, isAdmin(currentUser)));
    }

    private boolean isAdmin(UserContext currentUser) {
        return currentUser.roles().stream().anyMatch(role -> "ADMIN".equalsIgnoreCase(role));
    }
}
