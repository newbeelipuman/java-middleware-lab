package study.middleware.mqreliability.web;

import java.time.Instant;
import java.util.List;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import study.middleware.mqreliability.config.ReliabilityProperties;
import study.middleware.mqreliability.notification.NotificationTaskRepository;
import study.middleware.mqreliability.notification.NotificationTask;
import study.middleware.mqreliability.outbox.OutboxEvent;
import study.middleware.mqreliability.outbox.OutboxRepository;
import study.middleware.mqreliability.quarantine.QuarantineRecord;
import study.middleware.mqreliability.quarantine.QuarantineRepository;
import study.middleware.mqreliability.metrics.ReliabilityMetrics;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final OutboxRepository outbox;
    private final NotificationTaskRepository tasks;
    private final QuarantineRepository quarantine;
    private final RocketMQTemplate template;
    private final ReliabilityProperties properties;
    private final ReliabilityMetrics metrics;

    public AdminController(
            OutboxRepository outbox,
            NotificationTaskRepository tasks,
            QuarantineRepository quarantine,
            RocketMQTemplate template,
            ReliabilityProperties properties,
            ReliabilityMetrics metrics
    ) {
        this.outbox = outbox;
        this.tasks = tasks;
        this.quarantine = quarantine;
        this.template = template;
        this.properties = properties;
        this.metrics = metrics;
    }

    @GetMapping("/outbox")
    public List<OutboxEvent> outbox() {
        return outbox.findAll();
    }

    @GetMapping("/quarantine")
    public List<QuarantineRecord> quarantine() {
        return quarantine.findAll();
    }

    @GetMapping("/tasks")
    public List<NotificationTask> tasks() {
        return tasks.findAll();
    }

    @PostMapping("/outbox/{id}/reactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reactivateOutbox(@PathVariable long id) {
        if (outbox.reactivate(id, Instant.now()) == 0) {
            throw new IllegalStateException("Only DEAD outbox events can be reactivated");
        }
    }

    @PostMapping("/tasks/{id}/reactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reactivateTask(@PathVariable long id) {
        if (tasks.reactivate(id, Instant.now()) == 0) {
            throw new IllegalStateException("Only DEAD notification tasks can be reactivated");
        }
    }

    @PostMapping("/quarantine/{id}/replay")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void replay(@PathVariable long id) {
        QuarantineRecord record = quarantine.find(id)
                .orElseThrow(() -> new ResourceNotFoundException("quarantine", id));
        if (!quarantine.markReplayed(id, record.replayCount())) {
            throw new IllegalStateException("Concurrent replay already won");
        }
        template.syncSend(properties.destination(),
                MessageBuilder.withPayload(record.payloadJson())
                        .setHeader("KEYS", record.eventId())
                        .setHeader("REPLAY_SOURCE", record.sourceMessageId()).build());
        metrics.count("mq_quarantine_replay_total", "sent");
    }
}
