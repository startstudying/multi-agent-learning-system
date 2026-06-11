package com.learningos.rag.parser;

final class ParserTextSanitizer {

    private ParserTextSanitizer() {
    }

    static String clean(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\uFEFF", "")
                .replace('\u0000', ' ')
                .replaceAll("\\p{Cntrl}", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    static void appendCleaned(StringBuilder builder, String text) {
        String cleaned = clean(text);
        if (cleaned.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(' ');
        }
        if (builder.length() + cleaned.length() > ParserResourceLimits.MAX_EXTRACTED_CHARS) {
            throw new IllegalArgumentException("PARSER_OUTPUT_LIMIT_EXCEEDED");
        }
        builder.append(cleaned);
    }
}
