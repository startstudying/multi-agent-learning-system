package com.learningos.rag.parser;

final class ParserResourceLimits {

    static final int MAX_INPUT_BYTES = 20 * 1024 * 1024;
    static final int MAX_PDF_PAGES = 500;
    static final int MAX_DOCX_PARAGRAPHS = 5000;
    static final int MAX_EXTRACTED_CHARS = 2_000_000;

    private ParserResourceLimits() {
    }

    static void requireInputWithinLimit(ParseInput input) {
        long sizeBytes = input == null ? 0 : input.sizeBytes();
        int actualBytes = input == null ? 0 : input.bytes().length;
        if (sizeBytes > MAX_INPUT_BYTES || actualBytes > MAX_INPUT_BYTES) {
            throw new IllegalArgumentException("PARSER_INPUT_LIMIT_EXCEEDED");
        }
    }
}
