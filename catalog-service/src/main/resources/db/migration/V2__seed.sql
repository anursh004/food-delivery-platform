CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Seed restaurant with a fixed UUID for e2e tests
INSERT INTO restaurants (id, name, city) VALUES
  ('00000000-0000-0000-0000-000000000001', 'Seed Bistro', 'Pune')
ON CONFLICT (id) DO NOTHING;

INSERT INTO menus (id, restaurant_id, item_name, price, is_available) VALUES
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Masala Dosa', 120.00, true),
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Paneer Tikka', 220.00, true)
ON CONFLICT DO NOTHING;
