package study.middleware.redisspecialization.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class CacheConsistencyRaceTest {

    @Test
    void oldDatabaseReadCanBeWrittenBackAfterUpdateDeletesCache() throws Exception {
        AtomicReference<String> database = new AtomicReference<>("old");
        AtomicReference<String> cache = new AtomicReference<>();
        CountDownLatch oldValueRead = new CountDownLatch(1);
        CountDownLatch updateCompleted = new CountDownLatch(1);

        Thread reader = new Thread(() -> {
            String staleValue = database.get();
            oldValueRead.countDown();
            await(updateCompleted);
            cache.set(staleValue);
        });
        reader.start();

        assertThat(oldValueRead.await(2, TimeUnit.SECONDS)).isTrue();
        database.set("new");
        cache.set(null);
        updateCompleted.countDown();
        reader.join();

        assertThat(database).hasValue("new");
        assertThat(cache).hasValue("old");
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
