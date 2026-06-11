package study.middleware.redisspecialization.invalidation;

record InvalidationTask(long id, String cacheKey, int retryCount) {
}
