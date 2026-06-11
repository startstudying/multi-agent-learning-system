package com.learningos.tutor.api;

import com.learningos.common.api.ApiResponse;
import com.learningos.common.auth.CurrentUserService;
import com.learningos.common.auth.UserContext;
import com.learningos.rag.api.dto.RagQueryDtos.RagQueryResponse;
import com.learningos.rag.application.RagQueryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tutor")
public class TutorController {

    private final RagQueryService ragQueryService;
    private final CurrentUserService currentUserService;

    public TutorController(RagQueryService ragQueryService, CurrentUserService currentUserService) {
        this.ragQueryService = ragQueryService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/ask")
    public ApiResponse<RagQueryResponse> ask(@Valid @RequestBody TutorAskRequest request) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(ragQueryService.query(
                currentUser.userId(),
                hasRole(currentUser, "ADMIN"),
                hasRole(currentUser, "TEACHER"),
                request.kbIds(),
                request.question(),
                request.topK()
        ));
    }

    @GetMapping(value = "/sessions/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @PathVariable String sessionId,
            @RequestParam String question,
            @RequestParam List<String> kbIds
    ) {
        SseEmitter emitter = new SseEmitter(10_000L);
        UserContext currentUser = currentUserService.currentUser();
        Thread.startVirtualThread(() -> {
            Instant startedAt = Instant.now();
            try {
                send(emitter, "status", Map.of("stage", "QUERY_REWRITE"));
                send(emitter, "status", Map.of("stage", "RETRIEVING"));
                RagQueryResponse response = ragQueryService.query(
                        currentUser.userId(),
                        hasRole(currentUser, "ADMIN"),
                        hasRole(currentUser, "TEACHER"),
                        kbIds,
                        question,
                        null
                );
                send(emitter, "status", Map.of("stage", "RERANKING"));
                send(emitter, "status", Map.of("stage", "GENERATING"));
                send(emitter, "token", Map.of("text", response.answer()));
                send(emitter, "done", Map.of(
                        "sources", response.sources(),
                        "retrieval", response.retrieval(),
                        "traceId", response.traceId(),
                        "latencyMs", Duration.between(startedAt, Instant.now()).toMillis(),
                        "sessionId", sessionId
                ));
                emitter.complete();
            } catch (Exception exception) {
                emitter.completeWithError(exception);
            }
        });
        return emitter;
    }

    private void send(SseEmitter emitter, String event, Object data) throws IOException {
        emitter.send(SseEmitter.event().name(event).data(data));
    }

    private boolean hasRole(UserContext currentUser, String role) {
        return currentUser.roles().stream().anyMatch(existingRole -> role.equalsIgnoreCase(existingRole));
    }

    public record TutorAskRequest(
            @NotBlank String question,
            @NotEmpty List<String> kbIds,
            Integer topK
    ) {
    }
}
