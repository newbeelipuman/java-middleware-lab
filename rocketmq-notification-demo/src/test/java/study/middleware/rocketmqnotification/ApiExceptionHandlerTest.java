package study.middleware.rocketmqnotification;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    @Test
    void returnsHttp429ForSentinelBlock() {
        ApiExceptionHandler handler = new ApiExceptionHandler();

        ResponseEntity<RateLimitResponse> response = handler.handleRateLimit(
                new SentinelRateLimitException(SentinelResources.DOCTOR_SEARCH, "default", new RuntimeException())
        );

        assertThat(response.getStatusCode().value()).isEqualTo(429);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("RATE_LIMITED");
        assertThat(response.getBody().resource()).isEqualTo(SentinelResources.DOCTOR_SEARCH);
    }
}
