package study.middleware.redisspecialization.invalidation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "app.invalidation",
        name = "scheduling-enabled",
        havingValue = "true",
        matchIfMissing = true
)
class InvalidationTaskScheduler {

    private final InvalidationTaskProcessor processor;

    InvalidationTaskScheduler(InvalidationTaskProcessor processor) {
        this.processor = processor;
    }

    @Scheduled(fixedDelayString = "${app.invalidation.fixed-delay:5s}")
    void processDue() {
        processor.processDue();
    }
}
