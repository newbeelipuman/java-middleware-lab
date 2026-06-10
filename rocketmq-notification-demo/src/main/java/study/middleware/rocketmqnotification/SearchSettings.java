package study.middleware.rocketmqnotification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SearchSettings {

    private final AtomicInteger defaultPageSize;

    public SearchSettings(@Value("${app.search.default-page-size:10}") int defaultPageSize) {
        this.defaultPageSize = new AtomicInteger(defaultPageSize);
    }

    public int defaultPageSize() {
        return defaultPageSize.get();
    }

    public void updateDefaultPageSize(int defaultPageSize) {
        if (defaultPageSize < 1 || defaultPageSize > 100) {
            throw new IllegalArgumentException("default page size must be between 1 and 100");
        }
        this.defaultPageSize.set(defaultPageSize);
    }
}
