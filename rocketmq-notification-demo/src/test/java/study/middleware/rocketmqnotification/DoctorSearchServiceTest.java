package study.middleware.rocketmqnotification;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DoctorSearchServiceTest {

    private final DoctorRepository doctorRepository = mock(DoctorRepository.class);
    private final DoctorSearchIndex doctorSearchIndex = mock(DoctorSearchIndex.class);
    private final DoctorSearchService doctorSearchService = new DoctorSearchService(doctorRepository, doctorSearchIndex);

    @Test
    void rebuildsIndexFromMysqlDoctors() {
        List<Doctor> doctors = List.of(doctor(1L, "Li Ming"));
        when(doctorRepository.findAll()).thenReturn(doctors);
        when(doctorSearchIndex.rebuild(doctors)).thenReturn(1);

        RebuildIndexResponse response = doctorSearchService.rebuildIndex();

        assertThat(response.indexedCount()).isEqualTo(1);
        verify(doctorSearchIndex).rebuild(doctors);
    }

    @Test
    void delegatesValidPagedSearchToIndex() {
        SearchPage<Doctor> page = SearchPage.of(List.of(doctor(1L, "Li Ming")), 0, 10, 1);
        when(doctorSearchIndex.search("cardiology", 0, 10)).thenReturn(page);

        SearchPage<Doctor> result = doctorSearchService.search("cardiology", 0, 10);

        assertThat(result).isEqualTo(page);
    }

    @Test
    void rejectsInvalidPagingParameters() {
        assertThatThrownBy(() -> doctorSearchService.search("cardiology", -1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("page");

        assertThatThrownBy(() -> doctorSearchService.search("cardiology", 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size");

        assertThatThrownBy(() -> doctorSearchService.search("cardiology", 0, 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size");
    }

    private Doctor doctor(long id, String name) {
        return new Doctor(
                id,
                name,
                "Cardiology",
                "hypertension and coronary disease",
                true,
                Instant.parse("2026-06-03T00:00:00Z")
        );
    }
}
