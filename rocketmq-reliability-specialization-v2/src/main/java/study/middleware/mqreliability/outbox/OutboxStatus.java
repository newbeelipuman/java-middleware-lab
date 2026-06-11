package study.middleware.mqreliability.outbox;

public enum OutboxStatus {
    NEW, PROCESSING, SENT, FAILED, DEAD
}
