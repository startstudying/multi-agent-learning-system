package com.learningos.rag.parser;

public interface OcrFallbackProvider {

    String provider();

    OcrFallbackResult extractText(ParseInput input);
}
