package com.learningos.rag.parser;

public record OcrFallbackResult(Status status, String reasonCode, String text, Double confidence) {

    public enum Status {
        DISABLED,
        UNAVAILABLE,
        SUCCEEDED,
        FAILED
    }

    public OcrFallbackResult(Status status, String reasonCode, String text) {
        this(status, reasonCode, text, null);
    }

    public OcrFallbackResult {
        status = status == null ? Status.UNAVAILABLE : status;
        reasonCode = reasonCode == null ? "" : reasonCode;
        text = text == null ? "" : text;
        confidence = normalizeConfidence(confidence);
    }

    private static Double normalizeConfidence(Double value) {
        if (value == null || !Double.isFinite(value) || value < 0 || value > 1) {
            return null;
        }
        return value;
    }
}
