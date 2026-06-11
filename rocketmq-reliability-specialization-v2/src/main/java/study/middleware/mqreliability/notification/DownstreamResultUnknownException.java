package study.middleware.mqreliability.notification;

public class DownstreamResultUnknownException extends RuntimeException {
    public DownstreamResultUnknownException(String message) {
        super(message);
    }
}
