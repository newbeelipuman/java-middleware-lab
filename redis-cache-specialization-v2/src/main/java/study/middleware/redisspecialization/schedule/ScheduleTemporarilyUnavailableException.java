package study.middleware.redisspecialization.schedule;

public class ScheduleTemporarilyUnavailableException extends RuntimeException {

    public ScheduleTemporarilyUnavailableException(long id) {
        super("Schedule temporarily unavailable while cache fallback is saturated: " + id);
    }
}
