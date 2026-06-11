package com.learningos.rag.parser;

import java.util.List;

public record ParsedDocument(DocumentParser parser, List<ParsedSection> sections) {
    public ParsedDocument {
        if (parser == null) {
            throw new IllegalArgumentException("parser is required");
        }
        sections = withReadingOrder(sections);
    }

    private static List<ParsedSection> withReadingOrder(List<ParsedSection> input) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }
        java.util.ArrayList<ParsedSection> ordered = new java.util.ArrayList<>(input.size());
        for (int index = 0; index < input.size(); index++) {
            ParsedSection section = input.get(index);
            if (section != null) {
                ordered.add(section.withReadingOrderIndex(index + 1));
            }
        }
        return List.copyOf(ordered);
    }
}
