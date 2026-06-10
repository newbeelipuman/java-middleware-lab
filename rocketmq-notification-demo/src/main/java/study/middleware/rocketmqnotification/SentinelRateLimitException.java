package study.middleware.rocketmqnotification;

public class SentinelRateLimitException extends RuntimeException {

    private final String resource;
    private final String limitApp;

    public SentinelRateLimitException(String resource, String limitApp, Throwable cause) {
        super("Request blocked by Sentinel resource=" + resource, cause);
        this.resource = resource;
        this.limitApp = limitApp;
    }

    public String resource() {
        return resource;
    }

    public String limitApp() {
        return limitApp;
    }
}
