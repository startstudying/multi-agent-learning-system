package com.learningos.rag.parser;

public class DocumentParseException extends RuntimeException {

    private final String safeCode;

    public DocumentParseException(String safeCode, Throwable cause) {
        super(safeCode, cause);
        this.safeCode = safeCode;
    }

    public String safeCode() {
        return safeCode;
    }
}
