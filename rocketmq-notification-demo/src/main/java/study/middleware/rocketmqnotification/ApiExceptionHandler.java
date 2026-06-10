package study.middleware.rocketmqnotification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(SentinelRateLimitException.class)
    public ResponseEntity<RateLimitResponse> handleRateLimit(SentinelRateLimitException ex) {
        log.warn("Sentinel blocked request resource={} limitApp={}", ex.resource(), ex.limitApp());
        RateLimitResponse response = new RateLimitResponse(
                "RATE_LIMITED",
                "Request was blocked by Sentinel flow control. Please retry later.",
                ex.resource(),
                "local-qps-rule"
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }
}
