package study.middleware.redisspecialization.cache;

public interface CachePolicyExecutor<T> {

    CachePolicy policy();

    T load(String cacheKey);
}
