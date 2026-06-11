package com.learningos.common.exception;

import com.learningos.common.api.ApiResponse;
import com.learningos.common.api.ErrorCode;
import com.learningos.common.trace.StructuredRequestLoggingFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Object>> handleApiException(ApiException exception, HttpServletRequest request) {
        ErrorCode errorCode = exception.getErrorCode();
        request.setAttribute(StructuredRequestLoggingFilter.ERROR_CODE_ATTRIBUTE, errorCode.name());
        return ResponseEntity.status(errorCode.httpStatus())
                .body(ApiResponse.of(errorCode, exception.getMessage(), null));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiResponse<Object>> handleValidationException(Exception exception, HttpServletRequest request) {
        request.setAttribute(StructuredRequestLoggingFilter.ERROR_CODE_ATTRIBUTE, ErrorCode.VALIDATION_ERROR.name());
        String message = extractValidationMessage(exception);
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.httpStatus())
                .body(ApiResponse.of(ErrorCode.VALIDATION_ERROR, message, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpectedException(Exception exception, HttpServletRequest request) {
        request.setAttribute(StructuredRequestLoggingFilter.ERROR_CODE_ATTRIBUTE, ErrorCode.INTERNAL_ERROR.name());
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.httpStatus())
                .body(ApiResponse.of(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMessage(), null));
    }

    private String extractValidationMessage(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException methodArgumentNotValidException
                && methodArgumentNotValidException.getBindingResult().getFieldErrors().stream().findFirst().isPresent()) {
            String defaultMessage = methodArgumentNotValidException.getBindingResult()
                    .getFieldErrors()
                    .stream()
                    .findFirst()
                    .map(fieldError -> fieldError.getDefaultMessage())
                    .orElse(null);
            if (defaultMessage != null && !defaultMessage.isBlank()) {
                return defaultMessage;
            }
        }
        if (exception instanceof BindException bindException
                && bindException.getBindingResult().getFieldErrors().stream().findFirst().isPresent()) {
            String defaultMessage = bindException.getBindingResult()
                    .getFieldErrors()
                    .stream()
                    .findFirst()
                    .map(fieldError -> fieldError.getDefaultMessage())
                    .orElse(null);
            if (defaultMessage != null && !defaultMessage.isBlank()) {
                return defaultMessage;
            }
        }
        if (exception instanceof ConstraintViolationException constraintViolationException
                && constraintViolationException.getConstraintViolations().stream().findFirst().isPresent()) {
            String message = constraintViolationException.getConstraintViolations()
                    .stream()
                    .findFirst()
                    .map(violation -> violation.getMessage())
                    .orElse(null);
            if (message != null && !message.isBlank()) {
                return message;
            }
        }
        return ErrorCode.VALIDATION_ERROR.defaultMessage();
    }
}
