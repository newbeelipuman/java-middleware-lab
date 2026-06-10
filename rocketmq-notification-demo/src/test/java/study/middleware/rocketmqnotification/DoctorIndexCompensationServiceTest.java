package study.middleware.rocketmqnotification;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DoctorIndexCompensationServiceTest {

    @Test
    void rebuildsDoctorIndexFromMysqlSource() {
        DoctorSearchService doctorSearchService = mock(DoctorSearchService.class);
        when(doctorSearchService.rebuildIndex()).thenReturn(new RebuildIndexResponse(4));
        DoctorIndexCompensationService service = new DoctorIndexCompensationService(
                doctorSearchService,
                Clock.fixed(Instant.parse("2026-06-09T08:00:00Z"), ZoneOffset.UTC)
        );

        CompensationJobResponse response = service.compensate();

        assertThat(response.jobName()).isEqualTo(DoctorIndexCompensationService.JOB_NAME);
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.indexedCount()).isEqualTo(4);
        assertThat(response.handledAt()).isEqualTo(Instant.parse("2026-06-09T08:00:00Z"));
        verify(doctorSearchService).rebuildIndex();
    }
}
