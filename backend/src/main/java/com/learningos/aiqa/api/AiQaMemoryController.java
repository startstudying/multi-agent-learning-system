package com.learningos.aiqa.api;

import com.learningos.aiqa.api.dto.AiQaDtos.MemoryMessageResponse;
import com.learningos.aiqa.api.dto.AiQaDtos.MemoryMessageUpdateRequest;
import com.learningos.aiqa.api.dto.AiQaDtos.MemorySessionResponse;
import com.learningos.aiqa.application.memory.MemoryLifecycleService;
import com.learningos.common.api.ApiResponse;
import com.learningos.common.auth.CurrentUserService;
import com.learningos.common.auth.UserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai/memory")
public class AiQaMemoryController {

    private final MemoryLifecycleService memoryLifecycleService;
    private final CurrentUserService currentUserService;

    public AiQaMemoryController(MemoryLifecycleService memoryLifecycleService, CurrentUserService currentUserService) {
        this.memoryLifecycleService = memoryLifecycleService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/sessions")
    public ApiResponse<List<MemorySessionResponse>> listSessions() {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(memoryLifecycleService.listSessions(currentUser.userId()));
    }

    @PatchMapping("/messages/{messageId}")
    public ApiResponse<MemoryMessageResponse> updateMessage(
            @PathVariable String messageId,
            @Valid @RequestBody MemoryMessageUpdateRequest request
    ) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(memoryLifecycleService.toMessageResponse(
                memoryLifecycleService.updateMessageSummary(currentUser.userId(), messageId, request.summary())
        ));
    }

    @DeleteMapping("/messages/{messageId}")
    public ApiResponse<MemoryMessageResponse> deleteMessage(@PathVariable String messageId) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(memoryLifecycleService.toMessageResponse(
                memoryLifecycleService.deleteMessage(currentUser.userId(), messageId)
        ));
    }
}
