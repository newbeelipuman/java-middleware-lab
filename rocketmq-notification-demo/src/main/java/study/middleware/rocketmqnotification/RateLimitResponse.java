package study.middleware.rocketmqnotification;

public record RateLimitResponse(
        String code,
        String message,
        String resource,
        String reason
) {
}
