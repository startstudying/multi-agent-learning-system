package com.learningos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "learning-os.storage")
public record StorageProperties(
        String endpoint,
        String accessKey,
        String secretKey,
        String bucket
) {
    public boolean configured() {
        return hasText(endpoint) && hasText(accessKey) && hasText(secretKey) && hasText(bucket);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
