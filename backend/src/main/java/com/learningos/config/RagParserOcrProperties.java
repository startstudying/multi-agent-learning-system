package com.learningos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.time.Duration;

@ConfigurationProperties(prefix = "learning-os.rag.parser.ocr")
public record RagParserOcrProperties(
        boolean enabled,
        String provider,
        ProcessProperties process
) {
    private static final Duration DEFAULT_PROCESS_TIMEOUT = Duration.ofSeconds(10);
    private static final int DEFAULT_MAX_OUTPUT_CHARS = 200_000;

    public RagParserOcrProperties(boolean enabled, String provider) {
        this(enabled, provider, new ProcessProperties("", DEFAULT_PROCESS_TIMEOUT, DEFAULT_MAX_OUTPUT_CHARS));
    }

    @ConstructorBinding
    public RagParserOcrProperties {
        provider = provider == null || provider.isBlank() ? "none" : provider.trim();
        process = process == null
                ? new ProcessProperties("", DEFAULT_PROCESS_TIMEOUT, DEFAULT_MAX_OUTPUT_CHARS)
                : process;
    }

    public record ProcessProperties(
            String command,
            Duration timeout,
            int maxOutputChars
    ) {
        public ProcessProperties {
            command = command == null ? "" : command.trim();
            timeout = timeout == null || timeout.isZero() || timeout.isNegative()
                    ? DEFAULT_PROCESS_TIMEOUT
                    : timeout;
            maxOutputChars = maxOutputChars <= 0 ? DEFAULT_MAX_OUTPUT_CHARS : maxOutputChars;
        }
    }
}
