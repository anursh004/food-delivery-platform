CREATE TABLE IF NOT EXISTS driver_locations(
  id UUID PRIMARY KEY,
  order_id UUID,
  lat DOUBLE PRECISION,
  lon DOUBLE PRECISION,
  updated_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_driver_locations_order_time ON driver_locations(order_id, updated_at);
