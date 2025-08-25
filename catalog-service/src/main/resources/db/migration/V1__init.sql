CREATE TABLE IF NOT EXISTS restaurants(
  id UUID PRIMARY KEY,
  name VARCHAR(200),
  city VARCHAR(100)
);
CREATE TABLE IF NOT EXISTS menus(
  id UUID PRIMARY KEY,
  restaurant_id UUID REFERENCES restaurants(id),
  item_name VARCHAR(200),
  price NUMERIC(12,2),
  is_available BOOLEAN DEFAULT true
);
CREATE INDEX IF NOT EXISTS idx_menus_restaurant_id ON menus(restaurant_id);
