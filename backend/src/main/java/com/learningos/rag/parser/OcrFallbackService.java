package com.learningos.rag.parser;

public interface OcrFallbackService {

    OcrFallbackResult extractText(ParseInput input);
}
