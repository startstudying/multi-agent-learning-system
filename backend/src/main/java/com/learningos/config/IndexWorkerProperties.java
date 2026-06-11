package com.learningos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "learning-os.rag.index-worker")
public record IndexWorkerProperties(
        Boolean enabled,
        Duration fixedDelay,
        Integer batchSize,
        Duration leaseDuration,
        Duration retryBackoff,
        Integer maxRetryCount
) {
    public IndexWorkerProperties {
        if (enabled == null) {
            enabled = true;
        }
        if (fixedDelay == null || fixedDelay.isNegative() || fixedDelay.isZero()) {
            fixedDelay = Duration.ofSeconds(5);
        }
        if (batchSize == null || batchSize <= 0) {
            batchSize = 2;
        }
        if (leaseDuration == null || leaseDuration.isNegative() || leaseDuration.isZero()) {
            leaseDuration = Duration.ofMinutes(2);
        }
        if (retryBackoff == null || retryBackoff.isNegative() || retryBackoff.isZero()) {
            retryBackoff = Duration.ofSeconds(30);
        }
        if (maxRetryCount == null || maxRetryCount < 0) {
            maxRetryCount = 2;
        }
    }
}
