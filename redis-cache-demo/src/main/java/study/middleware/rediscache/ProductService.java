package study.middleware.rediscache;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class ProductService {

    private final ProductCache cache;
    private final ProductRepository repository;
    private final ConcurrentMap<Long, Object> loadLocks = new ConcurrentHashMap<>();

    public ProductService(ProductCache cache, ProductRepository repository) {
        this.cache = cache;
        this.repository = repository;
    }

    public Optional<Product> find(long id) {
        Optional<Product> cachedProduct = cache.find(id);
        if (cachedProduct.isPresent()) {
            return cachedProduct;
        }
        if (cache.isKnownMissing(id)) {
            return Optional.empty();
        }

        Object lock = loadLocks.computeIfAbsent(id, ignored -> new Object());
        try {
            synchronized (lock) {
                Optional<Product> productAfterLock = cache.find(id);
                if (productAfterLock.isPresent()) {
                    return productAfterLock;
                }
                if (cache.isKnownMissing(id)) {
                    return Optional.empty();
                }

                Optional<Product> product = repository.find(id);
                if (product.isPresent()) {
                    cache.put(product.get());
                } else {
                    cache.putMissing(id);
                }
                return product;
            }
        } finally {
            loadLocks.remove(id, lock);
        }
    }

    public Product update(long id, UpdateProductRequest request) {
        Product product = new Product(id, request.name(), request.price());
        repository.save(product);
        cache.evict(id);
        return product;
    }
}
