CREATE TABLE outbox (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  topic_name VARCHAR(255) NOT NULL,
  message_key VARCHAR(255),
  event_type VARCHAR(100) NOT NULL,
  source_service VARCHAR(100) NOT NULL,
  payload TEXT NOT NULL,
  headers JSONB,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  processed BOOLEAN NOT NULL DEFAULT false,
  failed BOOLEAN NOT NULL DEFAULT false,
  retry_count INTEGER NOT NULL DEFAULT 0,
  last_error TEXT
);