package study.middleware.rocketmqnotification;

import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DoctorSearchControllerTest {

    @BeforeEach
    void clearSentinelRules() {
        FlowRuleManager.loadRules(List.of());
    }

    @Test
    void usesNacosControlledDefaultPageSizeWhenSizeIsMissing() {
        DoctorSearchService doctorSearchService = mock(DoctorSearchService.class);
        SearchSettings searchSettings = new SearchSettings(10);
        searchSettings.updateDefaultPageSize(2);
        SearchPage<Doctor> expected = SearchPage.of(List.of(), 0, 2, 0);
        when(doctorSearchService.search("Cardiology", 0, 2)).thenReturn(expected);
        DoctorSearchController controller = new DoctorSearchController(
                doctorSearchService,
                searchSettings,
                new SentinelGuard()
        );

        SearchPage<Doctor> result = controller.search("Cardiology", 0, null);

        assertThat(result).isEqualTo(expected);
        verify(doctorSearchService).search("Cardiology", 0, 2);
    }
}
