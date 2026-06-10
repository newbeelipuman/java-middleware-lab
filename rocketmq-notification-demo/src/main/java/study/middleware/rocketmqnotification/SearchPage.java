package study.middleware.rocketmqnotification;

import java.util.List;

public record SearchPage<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> SearchPage<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new SearchPage<>(content, page, size, totalElements, totalPages);
    }
}
