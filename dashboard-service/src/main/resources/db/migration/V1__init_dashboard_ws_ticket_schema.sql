CREATE TABLE IF NOT EXISTS dashboard_read.ws_ticket_consumptions (
    jti UUID PRIMARY KEY,
    subject VARCHAR(64) NOT NULL,
    username VARCHAR(64),
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ws_ticket_consumptions_expires_at
    ON dashboard_read.ws_ticket_consumptions (expires_at);
