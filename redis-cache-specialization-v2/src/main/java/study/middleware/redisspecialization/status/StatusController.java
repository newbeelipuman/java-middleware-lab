package study.middleware.redisspecialization.status;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import study.middleware.redisspecialization.cache.CachePolicy;

@RestController
@RequestMapping("/api/status")
class StatusController {

    @GetMapping
    StatusResponse status() {
        List<String> policies = Arrays.stream(CachePolicy.values())
                .map(Enum::name)
                .toList();
        return new StatusResponse(
                "redis-cache-specialization-v2",
                "redis-cache-demo",
                "Stage 1 skeleton",
                policies
        );
    }
}
