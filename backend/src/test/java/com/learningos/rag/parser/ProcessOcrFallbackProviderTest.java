package com.learningos.rag.parser;

import com.learningos.config.RagParserOcrProperties;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessOcrFallbackProviderTest {

    @Test
    void commandMissingReturnsUnavailable() {
        ProcessOcrFallbackProvider provider = new ProcessOcrFallbackProvider(
                new RagParserOcrProperties.ProcessProperties("", Duration.ofSeconds(1), 1024)
        );

        OcrFallbackResult result = provider.extractText(input("irrelevant"));

        assertThat(result.status()).isEqualTo(OcrFallbackResult.Status.UNAVAILABLE);
        assertThat(result.reasonCode()).isEqualTo("OCR_PROVIDER_UNAVAILABLE");
        assertThat(result.text()).isEmpty();
    }

    @Test
    void commandSuccessReturnsStdoutText() {
        ProcessOcrFallbackProvider provider = new ProcessOcrFallbackProvider(
                commandProperties("python -c \"import sys; sys.stdin.buffer.read(); print('recognized scan text')\"")
        );

        OcrFallbackResult result = provider.extractText(input("%PDF image only"));

        assertThat(result.status()).isEqualTo(OcrFallbackResult.Status.SUCCEEDED);
        assertThat(result.reasonCode()).isEqualTo("OCR_PROVIDER_SUCCEEDED");
        assertThat(result.text()).isEqualTo("recognized scan text");
    }

    @Test
    void nonZeroExitReturnsSafeFailureWithoutStderrLeakage() {
        ProcessOcrFallbackProvider provider = new ProcessOcrFallbackProvider(
                commandProperties("python -c \"import sys; sys.stderr.write('C:/secret/scan.pdf apiKey=sk-live raw text'); sys.exit(7)\"")
        );

        OcrFallbackResult result = provider.extractText(input("%PDF image only"));

        assertThat(result.status()).isEqualTo(OcrFallbackResult.Status.FAILED);
        assertThat(result.reasonCode()).isEqualTo("OCR_PROVIDER_FAILED");
        assertThat(result.text()).isEmpty();
        assertThat(result.toString()).doesNotContain("sk-live", "C:/secret", "raw text");
    }

    @Test
    void timeoutReturnsSafeFailure() {
        ProcessOcrFallbackProvider provider = new ProcessOcrFallbackProvider(
                new RagParserOcrProperties.ProcessProperties(
                        "python -c \"import time; time.sleep(5); print('late text')\"",
                        Duration.ofMillis(100),
                        1024
                )
        );

        OcrFallbackResult result = provider.extractText(input("%PDF image only"));

        assertThat(result.status()).isEqualTo(OcrFallbackResult.Status.FAILED);
        assertThat(result.reasonCode()).isEqualTo("OCR_PROVIDER_FAILED");
        assertThat(result.text()).isEmpty();
    }

    @Test
    void oversizedStdoutReturnsSafeFailure() {
        ProcessOcrFallbackProvider provider = new ProcessOcrFallbackProvider(
                new RagParserOcrProperties.ProcessProperties(
                        "python -c \"print('x' * 2048)\"",
                        Duration.ofSeconds(1),
                        128
                )
        );

        OcrFallbackResult result = provider.extractText(input("%PDF image only"));

        assertThat(result.status()).isEqualTo(OcrFallbackResult.Status.FAILED);
        assertThat(result.reasonCode()).isEqualTo("OCR_PROVIDER_FAILED");
        assertThat(result.text()).isEmpty();
    }

    @Test
    void configurableServiceCanSelectProcessProvider() {
        ProcessOcrFallbackProvider processProvider = new ProcessOcrFallbackProvider(
                commandProperties("python -c \"import sys; sys.stdin.buffer.read(); print('selected provider text')\"")
        );
        ConfigurableOcrFallbackService service = new ConfigurableOcrFallbackService(
                new RagParserOcrProperties(true, "process",
                        new RagParserOcrProperties.ProcessProperties("", Duration.ofSeconds(1), 1024)),
                java.util.List.of(processProvider)
        );

        OcrFallbackResult result = service.extractText(input("%PDF image only"));

        assertThat(result.status()).isEqualTo(OcrFallbackResult.Status.SUCCEEDED);
        assertThat(result.reasonCode()).isEqualTo("OCR_PROVIDER_SUCCEEDED");
        assertThat(result.text()).isEqualTo("selected provider text");
    }

    private RagParserOcrProperties.ProcessProperties commandProperties(String command) {
        return new RagParserOcrProperties.ProcessProperties(command, Duration.ofSeconds(2), 4096);
    }

    private ParseInput input(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        return new ParseInput("doc_ocr", "scan.pdf", "application/pdf", bytes.length, bytes);
    }
}
