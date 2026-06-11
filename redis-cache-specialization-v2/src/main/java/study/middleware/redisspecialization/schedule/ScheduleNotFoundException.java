package study.middleware.redisspecialization.schedule;

public class ScheduleNotFoundException extends RuntimeException {

    public ScheduleNotFoundException(long id) {
        super("Schedule not found: " + id);
    }
}
