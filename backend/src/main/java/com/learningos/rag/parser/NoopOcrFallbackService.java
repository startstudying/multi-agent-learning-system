package com.learningos.rag.parser;

public class NoopOcrFallbackService implements OcrFallbackService {

    @Override
    public OcrFallbackResult extractText(ParseInput input) {
        return new OcrFallbackResult(OcrFallbackResult.Status.DISABLED, "OCR_DISABLED", "");
    }
}
