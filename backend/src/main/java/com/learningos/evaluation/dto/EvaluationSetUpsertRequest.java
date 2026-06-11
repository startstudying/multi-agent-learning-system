package com.learningos.evaluation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record EvaluationSetUpsertRequest(
        @NotBlank @Size(max = 120) String code,
        @NotBlank @Size(max = 80) String version,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 12000) String description,
        @NotBlank @Size(max = 40) String type,
        @Size(max = 40) String status,
        @Size(max = 80) String courseId,
        @Size(max = 80) String kbId,
        @Size(max = 120) String promptCode,
        @Size(max = 120) String promptVersion,
        @Valid List<EvaluationSampleRequest> samples
) {
    public EvaluationSetUpsertRequest {
        samples = samples == null ? List.of() : List.copyOf(samples);
    }
}
