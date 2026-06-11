package study.middleware.redisspecialization.cache;

public class CacheAccessException extends RuntimeException {

    public CacheAccessException(String operation, Throwable cause) {
        super("Redis operation failed: " + operation, cause);
    }
}
