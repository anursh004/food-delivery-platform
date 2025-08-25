CREATE TABLE IF NOT EXISTS orders(
  id UUID PRIMARY KEY,
  customer_id VARCHAR(64),
  restaurant_id VARCHAR(64),
  total_amount NUMERIC(12,2),
  status VARCHAR(32),
  idempotency_key VARCHAR(128) UNIQUE,
  created_at TIMESTAMPTZ DEFAULT now()
);
CREATE TABLE IF NOT EXISTS outbox_events(
  id UUID PRIMARY KEY,
  aggregate_id UUID,
  type VARCHAR(64),
  payload JSONB,
  status VARCHAR(16) DEFAULT 'NEW',
  created_at TIMESTAMPTZ DEFAULT now(),
  published_at TIMESTAMPTZ
);
