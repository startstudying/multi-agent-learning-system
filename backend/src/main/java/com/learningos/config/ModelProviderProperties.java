package com.learningos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "learning-os.model-provider")
public record ModelProviderProperties(
        String encryptionKey
) {
}
