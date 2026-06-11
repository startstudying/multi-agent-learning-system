package com.learningos.agent.dto;

import com.learningos.agent.application.ModelProviderSecretCodec;
import com.learningos.agent.domain.ModelProvider;

import java.time.Instant;

public record ModelProviderResponse(
        String id,
        String providerCode,
        String displayName,
        String remark,
        String websiteUrl,
        String baseUrl,
        String chatModel,
        String embeddingModel,
        boolean apiKeyConfigured,
        String apiKeyMasked,
        boolean enabled,
        boolean defaultProvider,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static ModelProviderResponse from(ModelProvider provider, String decryptedApiKey) {
        return new ModelProviderResponse(
                provider.getId(),
                provider.getProviderCode(),
                provider.getDisplayName(),
                provider.getRemark(),
                provider.getWebsiteUrl(),
                provider.getBaseUrl(),
                provider.getChatModel(),
                provider.getEmbeddingModel(),
                decryptedApiKey != null && !decryptedApiKey.isBlank(),
                ModelProviderSecretCodec.maskApiKey(decryptedApiKey),
                provider.isEnabled(),
                provider.isDefaultProvider(),
                provider.getCreatedBy(),
                provider.getCreatedAt(),
                provider.getUpdatedAt()
        );
    }
}
