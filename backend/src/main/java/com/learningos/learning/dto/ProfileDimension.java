package com.learningos.learning.dto;

public record ProfileDimension(
        String name,
        String value,
        double confidence,
        String evidence,
        ProfileUpdateSourceType sourceType,
        String lastEvidenceId
) {
    public ProfileDimension(String name, String value, double confidence, String evidence) {
        this(name, value, confidence, evidence, ProfileUpdateSourceType.CONVERSATION, null);
    }

    public ProfileDimension(String name, String value, double confidence, String evidence, ProfileUpdateSourceType sourceType) {
        this(name, value, confidence, evidence, sourceType, null);
    }

    public ProfileUpdateSourceType sourceTypeOrDefault() {
        return sourceType == null ? ProfileUpdateSourceType.CONVERSATION : sourceType;
    }
}
