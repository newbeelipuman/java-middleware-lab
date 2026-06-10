package study.middleware.rocketmqnotification;

import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;

@Component
public class DoctorIndexCompensationJob {

    private final DoctorIndexCompensationService compensationService;

    public DoctorIndexCompensationJob(DoctorIndexCompensationService compensationService) {
        this.compensationService = compensationService;
    }

    @XxlJob(DoctorIndexCompensationService.JOB_NAME)
    public void runByXxlJob() {
        compensationService.compensate();
    }

    public CompensationJobResponse runManually() {
        return compensationService.compensate();
    }
}
