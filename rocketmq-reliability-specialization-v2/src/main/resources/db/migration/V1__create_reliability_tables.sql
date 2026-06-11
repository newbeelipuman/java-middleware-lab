CREATE TABLE shift_change_request (
    id VARCHAR(64) NOT NULL,
    staff_code VARCHAR(64) NOT NULL,
    from_shift VARCHAR(32) NOT NULL,
    to_shift VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE outbox_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    schema_version INT NOT NULL,
    trace_id VARCHAR(64),
    payload_json JSON NOT NULL,
    status VARCHAR(16) NOT NULL,
    worker_id VARCHAR(128),
    processing_deadline TIMESTAMP(6),
    fencing_token BIGINT NOT NULL DEFAULT 0,
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP(6) NOT NULL,
    last_error VARCHAR(500),
    created_at TIMESTAMP(6) NOT NULL,
    sent_at TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_outbox_event_id (event_id),
    KEY idx_outbox_claim (status, next_retry_at, processing_deadline)
);

CREATE TABLE transaction_event_fact (
    event_id VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64),
    transaction_state VARCHAR(16) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (event_id)
);

CREATE TABLE notification_task (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL,
    channel VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    worker_id VARCHAR(128),
    processing_deadline TIMESTAMP(6),
    fencing_token BIGINT NOT NULL DEFAULT 0,
    business_retry_count INT NOT NULL DEFAULT 0,
    mq_redelivery_count INT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP(6) NOT NULL,
    error_type VARCHAR(64),
    last_error VARCHAR(500),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_event_channel (event_id, channel),
    KEY idx_notification_claim (status, next_retry_at, processing_deadline)
);

CREATE TABLE downstream_delivery (
    id BIGINT NOT NULL AUTO_INCREMENT,
    idempotency_key VARCHAR(160) NOT NULL,
    result_code VARCHAR(32) NOT NULL,
    delivered_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_downstream_idempotency (idempotency_key)
);

CREATE TABLE quarantined_message (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source_message_id VARCHAR(128) NOT NULL,
    event_id VARCHAR(64),
    reason VARCHAR(128) NOT NULL,
    payload_json LONGTEXT NOT NULL,
    replay_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL,
    replayed_at TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_quarantine_source_message (source_message_id)
);
