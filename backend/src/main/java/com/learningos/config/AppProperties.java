package com.learningos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "learning-os.app")
public record AppProperties(
        String environment,
        String traceHeader
) {
    public AppProperties {
        if (environment == null || environment.isBlank()) {
            environment = "dev";
        }
        if (traceHeader == null || traceHeader.isBlank()) {
            traceHeader = "X-Trace-Id";
        }
    }
}
