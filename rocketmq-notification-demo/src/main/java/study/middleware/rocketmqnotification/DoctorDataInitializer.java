package study.middleware.rocketmqnotification;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class DoctorDataInitializer implements ApplicationRunner {

    private final DoctorRepository doctorRepository;

    public DoctorDataInitializer(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (doctorRepository.count() > 0) {
            return;
        }

        Instant updatedAt = Instant.parse("2026-06-03T00:00:00Z");
        List.of(
                new Doctor(null, "Li Ming", "Cardiology", "hypertension and coronary disease", true, updatedAt),
                new Doctor(null, "Wang Fang", "Neurology", "migraine and stroke rehabilitation", true, updatedAt),
                new Doctor(null, "Chen Yu", "Pediatrics", "child fever and respiratory infection", false, updatedAt),
                new Doctor(null, "Zhao Lin", "Orthopedics", "sports injury and joint pain", true, updatedAt)
        ).forEach(doctorRepository::saveNew);
    }
}
