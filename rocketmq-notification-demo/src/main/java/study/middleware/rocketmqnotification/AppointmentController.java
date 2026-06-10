package study.middleware.rocketmqnotification;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final NotificationRecordStore notificationRecordStore;
    private final SentinelGuard sentinelGuard;

    public AppointmentController(
            AppointmentService appointmentService,
            NotificationRecordStore notificationRecordStore,
            SentinelGuard sentinelGuard
    ) {
        this.appointmentService = appointmentService;
        this.notificationRecordStore = notificationRecordStore;
        this.sentinelGuard = sentinelGuard;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Appointment create(@RequestBody CreateAppointmentRequest request) {
        return sentinelGuard.protect(SentinelResources.APPOINTMENT_CREATE, () -> appointmentService.create(request));
    }

    @GetMapping("/{id}/notifications")
    public List<NotificationRecord> notifications(@PathVariable long id) {
        return notificationRecordStore.findByAppointmentId(id);
    }
}
