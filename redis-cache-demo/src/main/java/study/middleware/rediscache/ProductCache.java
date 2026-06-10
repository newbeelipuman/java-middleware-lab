package study.middleware.rediscache;

import java.util.Optional;

public interface ProductCache {

    Optional<Product> find(long id);

    boolean isKnownMissing(long id);

    void put(Product product);

    void putMissing(long id);

    void evict(long id);
}
