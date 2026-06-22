package com.learningos.aiqa.api;

import com.learningos.aiqa.api.dto.AiQaDtos.AiQaRequest;
import com.learningos.aiqa.api.dto.AiQaDtos.AiQaResponse;
import com.learningos.aiqa.application.AiQaService;
import com.learningos.common.api.ApiResponse;
import com.learningos.common.auth.CurrentUserService;
import com.learningos.common.auth.UserContext;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class AiQaController {

    private final AiQaService aiQaService;
    private final CurrentUserService currentUserService;

    public AiQaController(AiQaService aiQaService, CurrentUserService currentUserService) {
        this.aiQaService = aiQaService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/api/ai/qa")
    public ApiResponse<AiQaResponse> answer(@Valid @RequestBody AiQaRequest request) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(aiQaService.answer(
                currentUser.userId(),
                hasRole(currentUser, "ADMIN"),
                hasRole(currentUser, "TEACHER"),
                request
        ));
    }

    @PostMapping(
            value = "/api/ai/qa/stream",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter streamAnswer(@Valid @RequestBody AiQaRequest request) {
        UserContext currentUser = currentUserService.currentUser();
        SseEmitter emitter = new SseEmitter(10_000L);
        Thread.startVirtualThread(() -> {
            Instant startedAt = Instant.now();
            try {
                sendStatus(emitter, "INTENT_ROUTING");
                sendStatus(emitter, "RETRIEVING");
                sendStatus(emitter, "MEMORY_CONTEXT");
                sendStatus(emitter, "COMPOSING");
                sendStatus(emitter, "VERIFYING");
                AiQaResponse response = aiQaService.answer(
                        currentUser.userId(),
                        hasRole(currentUser, "ADMIN"),
                        hasRole(currentUser, "TEACHER"),
                        request
                );
                send(emitter, "token", Map.of("text", response.answer() == null ? "" : response.answer()));
                send(emitter, "done", streamDonePayload(response, Duration.between(startedAt, Instant.now()).toMillis()));
                emitter.complete();
            } catch (Exception exception) {
                sendSafeError(emitter);
            }
        });
        return emitter;
    }

    private Map<String, Object> streamDonePayload(AiQaResponse response, long latencyMs) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("answer", response.answer());
        payload.put("answerMode", response.answerMode());
        payload.put("reasoningEffort", response.reasoningEffort());
        payload.put("reasoningSummary", response.reasoningSummary());
        payload.put("sourceStatus", response.sourceStatus());
        payload.put("sourcePolicy", response.sourcePolicy());
        payload.put("sources", response.sources());
        payload.put("citations", response.citations());
        payload.put("learnerFit", response.learnerFit());
        payload.put("nextSteps", response.nextSteps());
        payload.put("uncertainty", response.uncertainty());
        payload.put("qualityFlags", response.qualityFlags());
        payload.put("requiresReview", response.requiresReview());
        payload.put("verification", response.verification());
        payload.put("traceId", response.traceId());
        payload.put("workflowId", response.workflowId());
        payload.put("toolCalls", response.toolCalls());
        payload.put("latencyMs", latencyMs);
        return payload;
    }

    private void sendStatus(SseEmitter emitter, String stage) throws IOException {
        send(emitter, "status", Map.of("stage", stage));
    }

    private void send(SseEmitter emitter, String event, Object data) throws IOException {
        emitter.send(SseEmitter.event().name(event).data(data));
    }

    private void sendSafeError(SseEmitter emitter) {
        try {
            send(emitter, "error", Map.of(
                    "code", "AI_QA_STREAM_FAILED",
                    "message", "AI QA stream failed"
            ));
            emitter.complete();
        } catch (IOException ioException) {
            emitter.completeWithError(ioException);
        }
    }

    private boolean hasRole(UserContext currentUser, String role) {
        return currentUser.roles().stream().anyMatch(existingRole -> role.equalsIgnoreCase(existingRole));
    }
}
