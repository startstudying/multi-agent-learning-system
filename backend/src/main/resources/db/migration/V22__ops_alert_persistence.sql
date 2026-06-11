CREATE TABLE IF NOT EXISTS ops_alert_record (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    alert_type VARCHAR(64) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    summary VARCHAR(512) NOT NULL,
    metrics_json TEXT NULL,
    reason_codes_json TEXT NULL,
    window_start TIMESTAMP(3) NOT NULL,
    window_end TIMESTAMP(3) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    acknowledged_by VARCHAR(64) NULL,
    acknowledged_at TIMESTAMP(3) NULL,
    notification_status VARCHAR(32) NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_ops_alert_window (alert_type, window_start, window_end)
);

CREATE INDEX idx_ops_alert_status ON ops_alert_record (status, updated_at);
