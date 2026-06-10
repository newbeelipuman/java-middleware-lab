package study.middleware.rocketmqnotification;

import java.time.Instant;

public record CompensationJobResponse(
        String jobName,
        String status,
        int indexedCount,
        Instant handledAt
) {
}
