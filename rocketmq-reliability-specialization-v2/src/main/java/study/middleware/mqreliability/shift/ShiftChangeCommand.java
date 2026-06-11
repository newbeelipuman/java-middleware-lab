package study.middleware.mqreliability.shift;

public interface ShiftChangeCommand {
    ShiftChange create(CreateShiftChangeRequest request);
}
