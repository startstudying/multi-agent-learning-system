package com.learningos.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(String code, String message, T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ErrorCode.OK.name(), ErrorCode.OK.defaultMessage(), data);
    }

    public static <T> ApiResponse<T> of(ErrorCode errorCode, String message, T data) {
        return new ApiResponse<>(errorCode.name(), message, data);
    }
}
