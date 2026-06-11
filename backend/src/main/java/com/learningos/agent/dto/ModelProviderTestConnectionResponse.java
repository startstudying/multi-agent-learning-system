package com.learningos.agent.dto;

public record ModelProviderTestConnectionResponse(
        String status,
        String providerCode,
        long latencyMs,
        String errorCode
) {
}
