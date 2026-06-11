package com.learningos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "learning-os.ops-alert")
public record OpsAlertProperties(
        boolean persistenceEnabled,
        boolean webhookEnabled,
        String webhookUrl
) {
    public OpsAlertProperties {
        webhookUrl = webhookUrl == null ? "" : webhookUrl.trim();
    }
}
