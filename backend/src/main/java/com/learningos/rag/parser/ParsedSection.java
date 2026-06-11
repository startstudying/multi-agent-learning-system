package com.learningos.rag.parser;

import java.util.List;

public record ParsedSection(
        String title,
        Integer headingLevel,
        List<String> headingPath,
        String content,
        Integer pageNum,
        String pageNumSource,
        Integer readingOrderIndex,
        Double layoutConfidence,
        Double ocrConfidence,
        String contentKind
) {
    public ParsedSection(
            String title,
            Integer headingLevel,
            List<String> headingPath,
            String content,
            Integer pageNum
    ) {
        this(
                title,
                headingLevel,
                headingPath,
                content,
                pageNum,
                pageNum == null ? "NONE" : "PARSER_INFERRED",
                null,
                null,
                null,
                "TEXT"
        );
    }

    public ParsedSection {
        headingPath = headingPath == null ? List.of() : List.copyOf(headingPath);
        content = content == null ? "" : content;
        pageNumSource = sanitizeShortCode(pageNumSource, pageNum == null ? "NONE" : "PARSER_INFERRED");
        readingOrderIndex = readingOrderIndex == null || readingOrderIndex < 1 ? null : readingOrderIndex;
        layoutConfidence = normalizeConfidence(layoutConfidence);
        ocrConfidence = normalizeConfidence(ocrConfidence);
        contentKind = sanitizeShortCode(contentKind, "TEXT");
    }

    public ParsedSection withReadingOrderIndex(int index) {
        if (readingOrderIndex != null) {
            return this;
        }
        return new ParsedSection(
                title,
                headingLevel,
                headingPath,
                content,
                pageNum,
                pageNumSource,
                index,
                layoutConfidence,
                ocrConfidence,
                contentKind
        );
    }

    private static Double normalizeConfidence(Double value) {
        if (value == null || !Double.isFinite(value) || value < 0 || value > 1) {
            return null;
        }
        return value;
    }

    private static String sanitizeShortCode(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toUpperCase(java.util.Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
        return normalized.isBlank() ? fallback : normalized;
    }
}
