package study.middleware.mqreliability.notification;

import java.time.Instant;
import org.springframework.stereotype.Service;
import study.middleware.mqreliability.config.ReliabilityProperties;
import study.middleware.mqreliability.event.DomainEventEnvelope;
import study.middleware.mqreliability.event.ShiftChangePayload;
import study.middleware.mqreliability.metrics.ReliabilityMetrics;

@Service
public class NotificationTaskService {
    private final NotificationTaskRepository tasks;
    private final DownstreamDeliveryService downstream;
    private final ReliabilityProperties properties;
    private final ReliabilityMetrics metrics;

    public NotificationTaskService(
            NotificationTaskRepository tasks,
            DownstreamDeliveryService downstream,
            ReliabilityProperties properties,
            ReliabilityMetrics metrics
    ) {
        this.tasks = tasks;
        this.downstream = downstream;
        this.properties = properties;
        this.metrics = metrics;
    }

    public void handle(DomainEventEnvelope<ShiftChangePayload> event, int redeliveryCount) {
        RuntimeException firstFailure = null;
        for (NotificationChannel channel : NotificationChannel.values()) {
            try {
                handleChannel(event, channel, redeliveryCount);
            } catch (RuntimeException exception) {
                if (firstFailure == null) firstFailure = exception;
            }
        }
        if (firstFailure != null) throw firstFailure;
    }

    private void handleChannel(
            DomainEventEnvelope<ShiftChangePayload> event,
            NotificationChannel channel,
            int redeliveryCount
    ) {
        Instant now = Instant.now();
        tasks.createIfAbsent(event.eventId(), channel, redeliveryCount, now);
        var claimed = tasks.claim(event.eventId(), channel, properties.workerId(),
                properties.processingTimeout(), now);
        if (claimed.isEmpty()) {
            NotificationTask current = tasks.find(event.eventId(), channel).orElseThrow();
            if ("SUCCESS".equals(current.status())) {
                metrics.count("mq_notification_total", "duplicate");
                return;
            }
            throw new IllegalStateException("Notification task is owned or waiting for retry");
        }
        NotificationTask task = claimed.get();
        try {
            downstream.deliver(event.eventId(), channel, event.payload());
            tasks.success(task, Instant.now());
            metrics.count("mq_notification_total", "success");
        } catch (RuntimeException exception) {
            tasks.fail(task, exception, properties.maxRetries(), Instant.now());
            metrics.count("mq_notification_total", "failed");
            throw exception;
        }
    }
}
