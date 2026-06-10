package study.middleware.rocketmqnotification;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DoctorSearchService {

    private final DoctorRepository doctorRepository;
    private final DoctorSearchIndex doctorSearchIndex;

    public DoctorSearchService(DoctorRepository doctorRepository, DoctorSearchIndex doctorSearchIndex) {
        this.doctorRepository = doctorRepository;
        this.doctorSearchIndex = doctorSearchIndex;
    }

    public RebuildIndexResponse rebuildIndex() {
        List<Doctor> doctors = doctorRepository.findAll();
        int indexed = doctorSearchIndex.rebuild(doctors);
        return new RebuildIndexResponse(indexed);
    }

    public SearchPage<Doctor> search(String keyword, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
        return doctorSearchIndex.search(keyword, page, size);
    }
}
