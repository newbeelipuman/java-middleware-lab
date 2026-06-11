package study.middleware.redisspecialization.invalidation;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class InvalidationTaskRepository {

    private final JdbcTemplate jdbcTemplate;

    public InvalidationTaskRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long create(String cacheKey) {
        jdbcTemplate.update(
                "INSERT INTO cache_invalidation_task(cache_key, status) VALUES (?, 'PENDING')",
                cacheKey
        );
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public List<Long> findDueIds(int limit) {
        return jdbcTemplate.queryForList(
                """
                SELECT id
                FROM cache_invalidation_task
                WHERE status IN ('PENDING', 'FAILED') AND next_retry_at <= CURRENT_TIMESTAMP(6)
                ORDER BY id
                LIMIT ?
                """,
                Long.class,
                limit
        );
    }

    public InvalidationTask claim(long id) {
        int claimed = jdbcTemplate.update(
                """
                UPDATE cache_invalidation_task
                SET status = 'PROCESSING'
                WHERE id = ? AND status IN ('PENDING', 'FAILED') AND next_retry_at <= CURRENT_TIMESTAMP(6)
                """,
                id
        );
        if (claimed == 0) {
            return null;
        }
        return jdbcTemplate.queryForObject(
                "SELECT id, cache_key, retry_count FROM cache_invalidation_task WHERE id = ?",
                (resultSet, rowNumber) -> new InvalidationTask(
                        resultSet.getLong("id"),
                        resultSet.getString("cache_key"),
                        resultSet.getInt("retry_count")
                ),
                id
        );
    }

    public void markSuccess(long id) {
        jdbcTemplate.update(
                "UPDATE cache_invalidation_task SET status = 'SUCCESS', last_error = NULL WHERE id = ?",
                id
        );
    }

    public void markFailure(InvalidationTask task, Throwable error, int maxRetries) {
        int nextRetryCount = task.retryCount() + 1;
        String status = nextRetryCount >= maxRetries ? "DEAD" : "FAILED";
        Duration delay = Duration.ofSeconds(Math.min(300, 1L << Math.min(nextRetryCount, 8)));
        jdbcTemplate.update(
                """
                UPDATE cache_invalidation_task
                SET status = ?, retry_count = ?, next_retry_at = ?, last_error = ?
                WHERE id = ?
                """,
                status,
                nextRetryCount,
                java.sql.Timestamp.from(Instant.now().plus(delay)),
                abbreviate(error.getMessage()),
                task.id()
        );
    }

    private String abbreviate(String message) {
        if (message == null) {
            return "unknown error";
        }
        return message.substring(0, Math.min(message.length(), 512));
    }
}
