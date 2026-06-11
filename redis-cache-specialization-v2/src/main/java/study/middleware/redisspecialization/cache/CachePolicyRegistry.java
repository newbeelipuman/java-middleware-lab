package study.middleware.redisspecialization.cache;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class CachePolicyRegistry {

    private final Map<CachePolicyType, CachePolicy> policies;

    public CachePolicyRegistry(List<CachePolicy> policies) {
        EnumMap<CachePolicyType, CachePolicy> indexed = new EnumMap<>(CachePolicyType.class);
        policies.forEach(policy -> indexed.put(policy.type(), policy));
        this.policies = Map.copyOf(indexed);
    }

    public CachePolicy get(CachePolicyType type) {
        CachePolicy policy = policies.get(type);
        if (policy == null) {
            throw new IllegalArgumentException("Unsupported cache policy: " + type);
        }
        return policy;
    }
}
