package com.learningos.agent.dto;

import com.learningos.agent.domain.ModelProvider;
import jakarta.validation.constraints.NotBlank;

public record ModelProviderUpsertRequest(
        @NotBlank String providerCode,
        @NotBlank String displayName,
        String remark,
        String websiteUrl,
        @NotBlank String baseUrl,
        String chatModel,
        String embeddingModel,
        String apiKey,
        Boolean enabled,
        Boolean defaultProvider
) {
}
