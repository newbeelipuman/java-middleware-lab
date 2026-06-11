package study.middleware.redisspecialization.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CachePolicyTest {

    @Test
    void exposesPlannedStageOnePolicies() {
        assertThat(CachePolicyType.values())
                .containsExactly(
                        CachePolicyType.NONE,
                        CachePolicyType.CACHE_ASIDE,
                        CachePolicyType.LOCAL_MUTEX,
                        CachePolicyType.REDISSON_LOCK,
                        CachePolicyType.LOGICAL_EXPIRE
                );
    }
}
