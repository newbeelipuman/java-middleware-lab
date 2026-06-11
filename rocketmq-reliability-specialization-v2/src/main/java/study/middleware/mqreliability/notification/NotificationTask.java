package study.middleware.mqreliability.notification;

public record NotificationTask(
        long id,
        String eventId,
        NotificationChannel channel,
        String status,
        String workerId,
        long fencingToken,
        int businessRetryCount
) {
}
