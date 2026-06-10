package study.middleware.rocketmqnotification;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DoctorIndexCompensationJobTest {

    @Test
    void manualAndXxlJobEntryShareSameCompensationService() {
        DoctorIndexCompensationService service = mock(DoctorIndexCompensationService.class);
        CompensationJobResponse expected = new CompensationJobResponse(
                DoctorIndexCompensationService.JOB_NAME,
                "COMPLETED",
                4,
                Instant.parse("2026-06-09T08:00:00Z")
        );
        when(service.compensate()).thenReturn(expected);
        DoctorIndexCompensationJob job = new DoctorIndexCompensationJob(service);

        assertThat(job.runManually()).isEqualTo(expected);
        job.runByXxlJob();

        verify(service, org.mockito.Mockito.times(2)).compensate();
    }
}
