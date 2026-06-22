package com.learningos.aiqa.application;

import com.learningos.aiqa.api.dto.AiQaDtos.AiQaRequest;
import com.learningos.aiqa.api.dto.AiQaDtos.AiQaResponse;
import com.learningos.aiqa.application.memory.MemoryLifecycleService;
import com.learningos.aiqa.application.runtime.QaRuntime;
import org.springframework.stereotype.Service;

@Service
public class AiQaService {

    private final QaRuntime qaRuntime;
    private final MemoryLifecycleService memoryLifecycleService;

    public AiQaService(QaRuntime qaRuntime, MemoryLifecycleService memoryLifecycleService) {
        this.qaRuntime = qaRuntime;
        this.memoryLifecycleService = memoryLifecycleService;
    }

    public AiQaResponse answer(
            String userId,
            boolean admin,
            boolean teacher,
            AiQaRequest request
    ) {
        AiQaResponse response = qaRuntime.run(userId, admin, teacher, request);
        memoryLifecycleService.recordQaTurn(userId, request, response);
        return response;
    }
}
