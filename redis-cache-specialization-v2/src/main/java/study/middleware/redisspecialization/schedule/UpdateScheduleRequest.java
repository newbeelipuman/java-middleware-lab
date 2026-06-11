package study.middleware.redisspecialization.schedule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateScheduleRequest(
        @NotBlank String department,
        @NotNull ShiftType shiftType
) {
}
