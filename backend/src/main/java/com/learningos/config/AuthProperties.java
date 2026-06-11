package com.learningos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "learning-os.auth")
public record AuthProperties(
        String jwtSecret,
        String issuer,
        String jwkSetUri,
        String audience,
        @DefaultValue("true") boolean devHeaderFallbackEnabled
) {
    public AuthProperties(String jwtSecret, String issuer) {
        this(jwtSecret, issuer, "", "", true);
    }

    @ConstructorBinding
    public AuthProperties {
        if (jwtSecret == null) {
            jwtSecret = "";
        }
        if (issuer == null || issuer.isBlank()) {
            issuer = "learning-os";
        }
        if (jwkSetUri == null) {
            jwkSetUri = "";
        }
        if (audience == null) {
            audience = "";
        }
    }
}
