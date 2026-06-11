package study.middleware.mqreliability.shift;

import jakarta.validation.constraints.NotBlank;

public record CreateShiftChangeRequest(
        @NotBlank String staffCode,
        @NotBlank String fromShift,
        @NotBlank String toShift
) {
}
