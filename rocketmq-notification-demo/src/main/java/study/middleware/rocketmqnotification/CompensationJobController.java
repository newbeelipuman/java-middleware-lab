package study.middleware.rocketmqnotification;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
public class CompensationJobController {

    private final DoctorIndexCompensationJob doctorIndexCompensationJob;

    public CompensationJobController(DoctorIndexCompensationJob doctorIndexCompensationJob) {
        this.doctorIndexCompensationJob = doctorIndexCompensationJob;
    }

    @PostMapping("/doctor-index-compensation/run")
    public CompensationJobResponse runDoctorIndexCompensation() {
        return doctorIndexCompensationJob.runManually();
    }
}
