package study.middleware.redisspecialization.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CachePolicyTest {

    @Test
    void exposesPlannedStageOnePolicies() {
        assertThat(CachePolicy.values())
                .containsExactly(
                        CachePolicy.NONE,
                        CachePolicy.CACHE_ASIDE,
                        CachePolicy.LOCAL_MUTEX,
                        CachePolicy.REDISSON_LOCK,
                        CachePolicy.LOGICAL_EXPIRE
                );
    }
}
