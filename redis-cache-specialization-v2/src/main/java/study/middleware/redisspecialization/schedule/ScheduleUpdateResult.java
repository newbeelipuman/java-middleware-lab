package study.middleware.redisspecialization.schedule;

public record ScheduleUpdateResult(StaffSchedule schedule, long invalidationTaskId) {
}
