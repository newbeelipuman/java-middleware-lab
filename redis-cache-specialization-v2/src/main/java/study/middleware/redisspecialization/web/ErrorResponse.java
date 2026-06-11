package study.middleware.redisspecialization.web;

public record ErrorResponse(String code, String message, String resource, String traceId) {
}
