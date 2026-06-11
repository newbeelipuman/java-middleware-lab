CREATE TABLE staff_schedule (
    id BIGINT NOT NULL PRIMARY KEY,
    staff_code VARCHAR(32) NOT NULL,
    department VARCHAR(64) NOT NULL,
    shift_date DATE NOT NULL,
    shift_type VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_staff_schedule_staff_date (staff_code, shift_date),
    KEY idx_staff_schedule_department_date (department, shift_date)
);

CREATE TABLE cache_invalidation_task (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    cache_key VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_error VARCHAR(512) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    KEY idx_cache_invalidation_due (status, next_retry_at)
);
