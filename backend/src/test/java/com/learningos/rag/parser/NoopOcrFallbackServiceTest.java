package com.learningos.rag.parser;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class NoopOcrFallbackServiceTest {

    @Test
    void noopOcrFallbackIsDisabledAndReturnsNoText() {
        NoopOcrFallbackService service = new NoopOcrFallbackService();
        ParseInput input = new ParseInput(
                "doc_9",
                "scan.pdf",
                "application/pdf",
                4L,
                "scan".getBytes(StandardCharsets.ISO_8859_1)
        );

        OcrFallbackResult result = service.extractText(input);

        assertThat(result.status()).isEqualTo(OcrFallbackResult.Status.DISABLED);
        assertThat(result.reasonCode()).isEqualTo("OCR_DISABLED");
        assertThat(result.text()).isEmpty();
    }
}
