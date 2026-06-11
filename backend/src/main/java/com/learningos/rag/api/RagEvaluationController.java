package com.learningos.rag.api;

import com.learningos.common.api.ApiResponse;
import com.learningos.rag.application.RagEvaluationRequest;
import com.learningos.rag.application.RagEvaluationResult;
import com.learningos.rag.application.RagEvaluationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag/evaluations")
public class RagEvaluationController {

    private final RagEvaluationService ragEvaluationService;

    public RagEvaluationController(RagEvaluationService ragEvaluationService) {
        this.ragEvaluationService = ragEvaluationService;
    }

    @PostMapping
    public ApiResponse<RagEvaluationResult> evaluate(@RequestBody RagEvaluationRequest request) {
        return ApiResponse.success(ragEvaluationService.evaluate(request));
    }
}
