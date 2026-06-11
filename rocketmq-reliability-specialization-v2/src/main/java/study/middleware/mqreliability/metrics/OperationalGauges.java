package study.middleware.mqreliability.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import study.middleware.mqreliability.notification.NotificationTaskRepository;
import study.middleware.mqreliability.outbox.OutboxRepository;
import study.middleware.mqreliability.quarantine.QuarantineRepository;

@Component
public class OperationalGauges {
    public OperationalGauges(
            MeterRegistry registry,
            OutboxRepository outbox,
            NotificationTaskRepository tasks,
            QuarantineRepository quarantine
    ) {
        Gauge.builder("mq_outbox_backlog", outbox, OutboxRepository::countBacklog).register(registry);
        Gauge.builder("mq_notification_backlog", tasks, NotificationTaskRepository::countBacklog).register(registry);
        Gauge.builder("mq_quarantine_messages", quarantine, QuarantineRepository::count).register(registry);
    }
}
