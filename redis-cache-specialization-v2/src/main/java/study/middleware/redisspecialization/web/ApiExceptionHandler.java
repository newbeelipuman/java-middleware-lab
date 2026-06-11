package study.middleware.redisspecialization.web;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import study.middleware.redisspecialization.schedule.ScheduleNotFoundException;
import study.middleware.redisspecialization.schedule.ScheduleTemporarilyUnavailableException;

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(ScheduleNotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(ScheduleNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "SCHEDULE_NOT_FOUND", exception.getMessage(), "schedule");
    }

    @ExceptionHandler(ScheduleTemporarilyUnavailableException.class)
    ResponseEntity<ErrorResponse> unavailable(ScheduleTemporarilyUnavailableException exception) {
        return response(
                HttpStatus.SERVICE_UNAVAILABLE,
                "CACHE_FALLBACK_SATURATED",
                exception.getMessage(),
                "schedule"
        );
    }

    private ResponseEntity<ErrorResponse> response(
            HttpStatus status,
            String code,
            String message,
            String resource
    ) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(code, message, resource, MDC.get("traceId")));
    }
}
