package study.middleware.rocketmqnotification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class DoctorIndexCompensationService {

    public static final String JOB_NAME = "doctorIndexCompensationJob";

    private static final Logger log = LoggerFactory.getLogger(DoctorIndexCompensationService.class);

    private final DoctorSearchService doctorSearchService;
    private final BusinessMetrics metrics;
    private final Clock clock;

    @Autowired
    public DoctorIndexCompensationService(DoctorSearchService doctorSearchService, BusinessMetrics metrics) {
        this(doctorSearchService, metrics, Clock.systemUTC());
    }

    DoctorIndexCompensationService(DoctorSearchService doctorSearchService, Clock clock) {
        this(doctorSearchService, BusinessMetrics.noop(), clock);
    }

    DoctorIndexCompensationService(DoctorSearchService doctorSearchService, BusinessMetrics metrics, Clock clock) {
        this.doctorSearchService = doctorSearchService;
        this.metrics = metrics;
        this.clock = clock;
    }

    public CompensationJobResponse compensate() {
        Timer.Sample sample = metrics.startTimer();
        try {
            RebuildIndexResponse response = doctorSearchService.rebuildIndex();
            Instant handledAt = Instant.now(clock);
            metrics.recordCompensationRun(JOB_NAME, "completed", sample);
            log.info("Doctor index compensation completed indexedCount={} handledAt={}", response.indexedCount(), handledAt);
            return new CompensationJobResponse(JOB_NAME, "COMPLETED", response.indexedCount(), handledAt);
        } catch (RuntimeException ex) {
            metrics.recordCompensationRun(JOB_NAME, "failed", sample);
            throw ex;
        }
    }
}
