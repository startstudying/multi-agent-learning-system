package com.learningos.rag.parser;

import com.learningos.config.RagParserOcrProperties;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurableOcrFallbackServiceTest {

    @Test
    void disabledConfigurationReturnsOcrDisabledWithoutCallingProvider() {
        RecordingOcrProvider provider = new RecordingOcrProvider("fake", input -> {
            throw new IllegalStateException("provider should not be called");
        });
        ConfigurableOcrFallbackService service = new ConfigurableOcrFallbackService(
                new RagParserOcrProperties(false, "fake"),
                List.of(provider)
        );

        OcrFallbackResult result = service.extractText(input());

        assertThat(result.status()).isEqualTo(OcrFallbackResult.Status.DISABLED);
        assertThat(result.reasonCode()).isEqualTo("OCR_DISABLED");
        assertThat(result.text()).isEmpty();
        assertThat(provider.called).isFalse();
    }

    @Test
    void enabledConfigurationWithoutMatchingProviderReturnsUnavailable() {
        ConfigurableOcrFallbackService service = new ConfigurableOcrFallbackService(
                new RagParserOcrProperties(true, "missing"),
                List.of(new RecordingOcrProvider("fake", input -> new OcrFallbackResult(
                        OcrFallbackResult.Status.SUCCEEDED,
                        "OCR_PROVIDER_SUCCEEDED",
                        "ocr text"
                )))
        );

        OcrFallbackResult result = service.extractText(input());

        assertThat(result.status()).isEqualTo(OcrFallbackResult.Status.UNAVAILABLE);
        assertThat(result.reasonCode()).isEqualTo("OCR_PROVIDER_UNAVAILABLE");
        assertThat(result.text()).isEmpty();
    }

    @Test
    void enabledConfigurationDelegatesToMatchingProvider() {
        ConfigurableOcrFallbackService service = new ConfigurableOcrFallbackService(
                new RagParserOcrProperties(true, "fake"),
                List.of(new RecordingOcrProvider("fake", input -> new OcrFallbackResult(
                        OcrFallbackResult.Status.SUCCEEDED,
                        "OCR_PROVIDER_SUCCEEDED",
                        "recognized text"
                )))
        );

        OcrFallbackResult result = service.extractText(input());

        assertThat(result.status()).isEqualTo(OcrFallbackResult.Status.SUCCEEDED);
        assertThat(result.reasonCode()).isEqualTo("OCR_PROVIDER_SUCCEEDED");
        assertThat(result.text()).isEqualTo("recognized text");
    }

    @Test
    void providerExceptionReturnsSafeFailureWithoutRawMessage() {
        ConfigurableOcrFallbackService service = new ConfigurableOcrFallbackService(
                new RagParserOcrProperties(true, "fake"),
                List.of(new RecordingOcrProvider("fake", input -> {
                    throw new IllegalStateException("C:\\secret\\scan.pdf apiKey=sk-live-secret raw learner text");
                }))
        );

        OcrFallbackResult result = service.extractText(input());

        assertThat(result.status()).isEqualTo(OcrFallbackResult.Status.FAILED);
        assertThat(result.reasonCode()).isEqualTo("OCR_PROVIDER_FAILED");
        assertThat(result.text()).isEmpty();
        assertThat(result.toString()).doesNotContain("sk-live-secret", "C:\\secret", "raw learner text");
    }

    @Test
    void unsafeProviderReasonCodeIsNormalized() {
        ConfigurableOcrFallbackService service = new ConfigurableOcrFallbackService(
                new RagParserOcrProperties(true, "fake"),
                List.of(new RecordingOcrProvider("fake", input -> new OcrFallbackResult(
                        OcrFallbackResult.Status.SUCCEEDED,
                        "ocr failed because apiKey=sk-live-secret",
                        "recognized text"
                )))
        );

        OcrFallbackResult result = service.extractText(input());

        assertThat(result.status()).isEqualTo(OcrFallbackResult.Status.SUCCEEDED);
        assertThat(result.reasonCode()).isEqualTo("OCR_PROVIDER_SUCCEEDED");
        assertThat(result.text()).isEqualTo("recognized text");
    }

    private ParseInput input() {
        return new ParseInput(
                "doc_ocr",
                "scan.pdf",
                "application/pdf",
                4L,
                "scan".getBytes(StandardCharsets.ISO_8859_1)
        );
    }

    private static class RecordingOcrProvider implements OcrFallbackProvider {

        private final String provider;
        private final OcrHandler handler;
        private boolean called;

        private RecordingOcrProvider(String provider, OcrHandler handler) {
            this.provider = provider;
            this.handler = handler;
        }

        @Override
        public String provider() {
            return provider;
        }

        @Override
        public OcrFallbackResult extractText(ParseInput input) {
            called = true;
            return handler.extractText(input);
        }
    }

    @FunctionalInterface
    private interface OcrHandler {
        OcrFallbackResult extractText(ParseInput input);
    }
}
