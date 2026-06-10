package study.middleware.rocketmqnotification;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompensationJobControllerTest {

    @Test
    void triggersDoctorIndexCompensationJob() {
        DoctorIndexCompensationJob job = mock(DoctorIndexCompensationJob.class);
        CompensationJobResponse expected = new CompensationJobResponse(
                DoctorIndexCompensationService.JOB_NAME,
                "COMPLETED",
                4,
                Instant.parse("2026-06-09T08:00:00Z")
        );
        when(job.runManually()).thenReturn(expected);
        CompensationJobController controller = new CompensationJobController(job);

        CompensationJobResponse response = controller.runDoctorIndexCompensation();

        assertThat(response).isEqualTo(expected);
    }
}
