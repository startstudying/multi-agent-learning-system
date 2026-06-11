package com.learningos.rag.application;

public enum RerankerStatus {
    NOT_CONFIGURED,
    SKIPPED_NO_CANDIDATES,
    SUCCEEDED,
    TIMEOUT_FALLBACK,
    ERROR_FALLBACK
}
