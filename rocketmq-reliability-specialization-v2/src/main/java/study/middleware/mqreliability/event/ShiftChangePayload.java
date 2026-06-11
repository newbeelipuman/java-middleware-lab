package study.middleware.mqreliability.event;

public record ShiftChangePayload(
        String requestId,
        String staffCode,
        String fromShift,
        String toShift
) {
}
