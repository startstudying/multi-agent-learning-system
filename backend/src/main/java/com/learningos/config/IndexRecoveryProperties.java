package com.learningos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "learning-os.rag.index-recovery")
public record IndexRecoveryProperties(
        Boolean enabled,
        Boolean runOnStartup,
        Duration runningTimeout,
        Duration fixedDelay
) {
    public IndexRecoveryProperties {
        if (enabled == null) {
            enabled = true;
        }
        if (runOnStartup == null) {
            runOnStartup = true;
        }
        if (runningTimeout == null || runningTimeout.isNegative() || runningTimeout.isZero()) {
            runningTimeout = Duration.ofMinutes(30);
        }
        if (fixedDelay == null || fixedDelay.isNegative() || fixedDelay.isZero()) {
            fixedDelay = Duration.ofMinutes(5);
        }
    }
}
