package study.middleware.mqreliability.web;

public record ApiError(String code, String message, String resource, String traceId) {
}
