package study.middleware.rocketmqnotification;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search/doctors")
public class DoctorSearchController {

    private final DoctorSearchService doctorSearchService;
    private final SearchSettings searchSettings;
    private final SentinelGuard sentinelGuard;

    public DoctorSearchController(
            DoctorSearchService doctorSearchService,
            SearchSettings searchSettings,
            SentinelGuard sentinelGuard
    ) {
        this.doctorSearchService = doctorSearchService;
        this.searchSettings = searchSettings;
        this.sentinelGuard = sentinelGuard;
    }

    @PostMapping("/rebuild-index")
    public RebuildIndexResponse rebuildIndex() {
        return doctorSearchService.rebuildIndex();
    }

    @GetMapping
    public SearchPage<Doctor> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size
    ) {
        return sentinelGuard.protect(
                SentinelResources.DOCTOR_SEARCH,
                () -> doctorSearchService.search(keyword, page, size == null ? searchSettings.defaultPageSize() : size)
        );
    }
}
