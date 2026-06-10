package study.middleware.rocketmqnotification;

public interface AppointmentEventPublisher {

    void publish(AppointmentCreatedEvent event);
}
