CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS customers (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    region TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    amount NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO customers(name, region) VALUES
('Acme Australia', 'APAC'),
('Northstar Labs', 'NA'),
('Berlin Retail', 'EMEA');

INSERT INTO orders(customer_id, amount, created_at) VALUES
(1, 1250.00, NOW() - INTERVAL '2 days'),
(1, 880.50, NOW() - INTERVAL '1 day'),
(2, 2300.00, NOW() - INTERVAL '4 days'),
(3, 740.25, NOW() - INTERVAL '3 days');
