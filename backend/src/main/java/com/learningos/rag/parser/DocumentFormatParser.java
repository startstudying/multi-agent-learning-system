package com.learningos.rag.parser;

import java.io.IOException;

public interface DocumentFormatParser {

    DocumentParser format();

    ParsedDocument parse(ParseInput input) throws IOException;
}
