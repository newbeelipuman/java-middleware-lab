package study.middleware.redisspecialization.cache;

public enum CachePolicy {
    NONE,
    CACHE_ASIDE,
    LOCAL_MUTEX,
    REDISSON_LOCK,
    LOGICAL_EXPIRE
}
