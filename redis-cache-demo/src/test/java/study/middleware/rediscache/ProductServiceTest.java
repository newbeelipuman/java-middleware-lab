package study.middleware.rediscache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    private final ProductCache cache = mock(ProductCache.class);
    private final ProductRepository repository = mock(ProductRepository.class);
    private final ProductService service = new ProductService(cache, repository);

    @Test
    void returnsCachedProductWithoutCallingRepository() {
        Product cached = new Product(1L, "cached", new BigDecimal("10.00"));
        when(cache.find(1L)).thenReturn(Optional.of(cached));

        assertThat(service.find(1L)).contains(cached);
        verify(repository, never()).find(1L);
    }

    @Test
    void loadsAndCachesProductOnCacheMiss() {
        Product stored = new Product(1L, "stored", new BigDecimal("20.00"));
        when(cache.find(1L)).thenReturn(Optional.empty());
        when(repository.find(1L)).thenReturn(Optional.of(stored));

        assertThat(service.find(1L)).contains(stored);
        verify(cache).put(stored);
    }

    @Test
    void doesNotCacheMissingProduct() {
        when(cache.find(99L)).thenReturn(Optional.empty());
        when(repository.find(99L)).thenReturn(Optional.empty());

        assertThat(service.find(99L)).isEmpty();
        verify(cache).putMissing(99L);
        verify(cache, never()).put(any());
    }

    @Test
    void returnsEmptyWithoutRepositoryWhenMissingValueIsCached() {
        when(cache.find(99L)).thenReturn(Optional.empty());
        when(cache.isKnownMissing(99L)).thenReturn(true);

        assertThat(service.find(99L)).isEmpty();
        verify(repository, never()).find(99L);
    }

    @Test
    void savesProductThenEvictsOldCacheValue() {
        UpdateProductRequest request = new UpdateProductRequest("updated", new BigDecimal("30.00"));
        Product expected = new Product(1L, "updated", new BigDecimal("30.00"));

        assertThat(service.update(1L, request)).isEqualTo(expected);
        verify(repository).save(expected);
        verify(cache).evict(1L);
    }

    @Test
    void protectsHotKeyFromRepeatedRepositoryLoadsOnConcurrentMiss() throws Exception {
        Product stored = new Product(1L, "stored", new BigDecimal("20.00"));
        ProductRepository slowRepository = mock(ProductRepository.class);
        FakeProductCache fakeCache = new FakeProductCache();
        ProductService hotKeyService = new ProductService(fakeCache, slowRepository);
        when(slowRepository.find(1L)).thenAnswer(invocation -> {
            Thread.sleep(80);
            return Optional.of(stored);
        });

        ExecutorService executor = Executors.newFixedThreadPool(6);
        CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < 6; i++) {
            executor.submit(() -> {
                start.await();
                assertThat(hotKeyService.find(1L)).contains(stored);
                return null;
            });
        }

        start.countDown();
        executor.shutdown();

        assertThat(executor.awaitTermination(2, TimeUnit.SECONDS)).isTrue();
        verify(slowRepository, times(1)).find(1L);
    }

    @Test
    void productCacheTtlHasJitterToReduceAvalancheRisk() {
        RedisProductCache redisProductCache = new RedisProductCache(
                null,
                new ObjectMapper(),
                Duration.ofMinutes(5),
                Duration.ofSeconds(30),
                Duration.ofSeconds(30)
        );

        for (int i = 0; i < 20; i++) {
            Duration ttl = redisProductCache.ttlWithJitter();
            assertThat(ttl).isBetween(Duration.ofMinutes(5), Duration.ofMinutes(5).plusSeconds(30));
        }
    }

    private static class FakeProductCache implements ProductCache {

        private final Map<Long, Product> products = new ConcurrentHashMap<>();
        private final Map<Long, Boolean> missing = new ConcurrentHashMap<>();

        @Override
        public Optional<Product> find(long id) {
            return Optional.ofNullable(products.get(id));
        }

        @Override
        public boolean isKnownMissing(long id) {
            return missing.containsKey(id);
        }

        @Override
        public void put(Product product) {
            products.put(product.id(), product);
        }

        @Override
        public void putMissing(long id) {
            missing.put(id, true);
        }

        @Override
        public void evict(long id) {
            products.remove(id);
            missing.remove(id);
        }
    }
}
