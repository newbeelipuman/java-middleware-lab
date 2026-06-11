package study.middleware.mqreliability.web;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> notFound(ResourceNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", exception);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalid(MethodArgumentNotValidException exception) {
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", exception);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> failure(Exception exception) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "OPERATION_FAILED", exception);
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String code, Exception exception) {
        return ResponseEntity.status(status).body(
                new ApiError(code, exception.getMessage(), "rocketmq-reliability-v2", MDC.get("traceId")));
    }
}
