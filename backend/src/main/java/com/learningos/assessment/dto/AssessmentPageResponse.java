package com.learningos.assessment.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record AssessmentPageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <T> AssessmentPageResponse<T> from(Page<T> page) {
        return new AssessmentPageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    public static <T> AssessmentPageResponse<T> empty(int page, int size) {
        return new AssessmentPageResponse<>(List.of(), page, size, 0, 0);
    }
}
