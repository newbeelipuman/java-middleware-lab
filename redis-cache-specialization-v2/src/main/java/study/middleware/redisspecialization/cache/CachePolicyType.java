package study.middleware.redisspecialization.cache;

public enum CachePolicyType {
    NONE,
    CACHE_ASIDE,
    LOCAL_MUTEX,
    REDISSON_LOCK,
    LOGICAL_EXPIRE
}
