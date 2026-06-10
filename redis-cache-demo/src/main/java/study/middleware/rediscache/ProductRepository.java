package study.middleware.rediscache;

import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ProductRepository {

    private final Map<Long, Product> products = new ConcurrentHashMap<>();

    public ProductRepository() {
        products.put(1L, new Product(1L, "Java Middleware Handbook", new BigDecimal("49.90")));
    }

    public Optional<Product> find(long id) {
        return Optional.ofNullable(products.get(id));
    }

    public Product save(Product product) {
        products.put(product.id(), product);
        return product;
    }
}

