package study.middleware.mqreliability.transaction;

import study.middleware.mqreliability.shift.CreateShiftChangeRequest;
import study.middleware.mqreliability.shift.ShiftChange;

public final class TransactionContext {
    private final String eventId;
    private final String aggregateId;
    private final CreateShiftChangeRequest request;
    private ShiftChange result;

    public TransactionContext(String eventId, String aggregateId, CreateShiftChangeRequest request) {
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.request = request;
    }

    public String eventId() {
        return eventId;
    }

    public String aggregateId() {
        return aggregateId;
    }

    public CreateShiftChangeRequest request() {
        return request;
    }

    public ShiftChange result() {
        return result;
    }

    public void result(ShiftChange result) {
        this.result = result;
    }
}
